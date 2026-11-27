#!/usr/bin/env python3
r"""SmartIsland Translation Generator and Validator.

Translates Android XML strings using LibreTranslate with robust protection for:
- Android format specifiers (%1$s, %2$d, %%, etc.)
- Brand names and protected terms (Smart Island, Shizuku, Open-Meteo, etc.)
- Newlines, tabs, and special characters
- Android XML escaping (\', \", \@, \?)
- Locale-specific typo prevention (e.g. Spanish 'via' -> 'vía')
- In-place validation to ensure translations never break Android Lint or Gradle builds.
"""

# Use this command to validate
# python scripts/translate_strings.py --validate-only

import argparse
import copy
import re
import sys
import time
import warnings
import xml.etree.ElementTree as ET
from pathlib import Path

# Suppress harmless urllib3/charset version mismatch warning from requests
warnings.filterwarnings("ignore", message="urllib3.*or chardet.*doesn't match a supported version")

import requests

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
SOURCE = RES / "values" / "strings.xml"
API = "http://127.0.0.1:5000/translate"

LANGUAGES = {
    "Brazilian Portuguese": ("pt", "values-pt-rBR"),
    "Spanish": ("es", "values-es"),
    "Simplified Chinese": ("zh", "values-zh-rCN"),
    "Japanese": ("ja", "values-ja"),
}

# Strict regex matching valid Java/Android Formatter conversion characters:
# %[argument_index$][flags][width][.precision]conversion
# Valid conversions: b, B, h, H, s, S, c, C, d, o, x, X, e, E, f, g, G, a, A, %, n, or t/T[a-zA-Z]
PLACEHOLDER = re.compile(
    r"%(?:\d+\$)?[-#+ 0,(<]*\d*(?:\.\d+)?(?:[bBhHsScCdoxXeEfgGaA%n]|[tT][a-zA-Z])"
)

PROTECTED_TERMS = [
    "Smart Island",
    "Shizuku",
    "Open-Meteo",
    "GitHub",
    "JSON",
    "APK",
    "Android",
    "Bluetooth",
    "Wi-Fi",
]


def sanitize_for_android_xml(text: str, lang: str = None) -> str:
    """Sanitize translated text for Android XML compatibility."""
    if not text:
        return text

    # Clean up corrupted placeholder spacing from MT (e.g. '% 1$s', '%1 $ d', '% s')
    # Use word boundary to avoid stripping spaces before normal words like '100% On-Device' or '100%% Solid'
    text = re.sub(r'%\s*(\d+)\s*\$\s*([bBhHsScCdoxXeEfgGaA%n])(?:\b|$)', r'%\1$\2', text)
    text = re.sub(r'%\s*(\d+)\s*\$\s*(\.\d+)?\s*([bBhHsScCdoxXeEfgGaA%n])(?:\b|$)', r'%\1$\2\3', text)
    text = re.sub(r'%\s+([bBhHsScCdoxXeEfgGaA%n])(?:\b|$)', r'%\1', text)

    # Clean up corrupted newline and tab escapes from MT (e.g. '\ n' -> '\n')
    text = re.sub(r'\\\s+n', r'\\n', text)
    text = re.sub(r'\\\s+t', r'\\t', text)

    # Clean up stray backslashes that are not valid Android escapes (n, t, ', ", \\, @, ?, u)
    def clean_slash(match):
        nxt = match.group(1)
        if nxt in ("n", "t", "'", '"', "\\", "@", "?", "u"):
            return "\\" + nxt
        return nxt

    text = re.sub(r'\\(.)', clean_slash, text)

    # Ensure unescaped single quotes are escaped with \'
    text = re.sub(r"(?<!\\)'", r"\'", text)

    # Ensure unescaped double quotes are escaped with \"
    text = re.sub(r'(?<!\\)"', r'\"', text)

    # Escape leading @ or ? so Android does not treat them as resource/theme references
    if text.startswith('@') and not text.startswith(r'\@'):
        text = r'\@' + text[1:]
    if text.startswith('?') and not text.startswith(r'\?'):
        text = r'\?' + text[1:]

    # Locale-specific corrections to prevent Android Lint Typos failures
    if lang == "es":
        # In Spanish, standalone 'via' is a misspelling of 'vía'
        text = re.sub(r'\bvia\b', 'vía', text)
        text = re.sub(r'\bVia\b', 'Vía', text)
        text = re.sub(r'\bVIA\b', 'VÍA', text)
        text = re.sub(r'\ba traves de\b', 'a través de', text, flags=re.IGNORECASE)
    elif lang == "pt":
        text = re.sub(r'\ba traves de\b', 'através de', text, flags=re.IGNORECASE)

    return text


def protect_text(text: str) -> tuple[str, dict[int, tuple[str, str]]]:
    """Tokenize format placeholders, escaped newlines, and protected terms."""
    protected = {}

    # Unescape common Android XML escapes for translation accuracy
    cleaned = text.replace(r"\'", "'").replace(r'\"', '"').replace(r"\\", "\\")

    # 1. Protect format placeholders (%1$s, %2$d, %%, etc.)
    def rep_ph(match):
        idx = len(protected)
        token = f"ZXQTOKEN{idx}QXZ"
        protected[idx] = (token, match.group())
        return token

    cleaned = PLACEHOLDER.sub(rep_ph, cleaned)

    # 2. Protect literal newlines and tabs
    def rep_esc(match):
        idx = len(protected)
        token = f"ZXQTOKEN{idx}QXZ"
        protected[idx] = (token, match.group())
        return token

    cleaned = re.sub(r'\\n|\\t', rep_esc, cleaned)

    # 3. Protect brand names and fixed terms
    for term in PROTECTED_TERMS:
        pattern = re.compile(rf"\b{re.escape(term)}\b", re.IGNORECASE)

        def rep_term(m):
            idx = len(protected)
            token = f"ZXQTOKEN{idx}QXZ"
            protected[idx] = (token, m.group())
            return token

        cleaned = pattern.sub(rep_term, cleaned)

    return cleaned, protected


def restore_text(translated: str, protected: dict[int, tuple[str, str]]) -> str:
    """Safely restore protected tokens even if the MT engine altered case or spacing."""
    result = translated

    # Pass 1: Direct exact replacements first (avoids adjacent token collisions)
    for idx, (token, original_val) in protected.items():
        if token in result:
            result = result.replace(token, original_val)

    # Pass 2: Bounded fuzzy regex for any tokens altered by MT (e.g. lowercase, spaces)
    for idx, (token, original_val) in protected.items():
        if original_val in result:
            continue
        pattern = re.compile(
            rf"Z[XQ]{{1,3}}[\s_]*(?:TOKEN|token|TOK|tok|トークン|托肯)?[\s_]*{idx}[\s_]*[QXZ]{{1,3}}",
            re.IGNORECASE,
        )
        result = pattern.sub(original_val, result)

    return result


def translate(text: str, language: str) -> str | None:
    """Translate text to the target language via LibreTranslate with error recovery."""
    if not text or not text.strip():
        return text

    original = text
    cleaned_input, protected = protect_text(text)

    for attempt in range(3):
        try:
            response = requests.post(
                API,
                data={
                    "q": cleaned_input,
                    "source": "en",
                    "target": language,
                    "format": "text",
                },
                timeout=60,
            )
            response.raise_for_status()
            result = response.json().get("translatedText", "")
            if not result:
                return None

            # Restore protected tokens
            result = restore_text(result, protected)

            # Verification: all placeholders must be restored and no token residue
            token_residue = re.search(r"Z[XQ].*?Q[XZ]", result, re.IGNORECASE)
            all_restored = all(val in result for _, val in protected.values())

            if token_residue or not all_restored:
                # If restoration failed, do not write a corrupt or half-translated string
                print(f"Warning: Token restoration incomplete for '{text[:40]}...'. Falling back.")
                return None

            return sanitize_for_android_xml(result, language)

        except requests.RequestException as error:
            if attempt == 2:
                print(f"Translation request failed: {error}")
                return None
            time.sleep(2)

    return None


def resource_key(element: ET.Element) -> tuple[str, str]:
    return element.tag, element.get("name", "")


def is_likely_untranslated(source_text: str, target_text: str, language: str) -> bool:
    """Check if a localized string was left unchanged in English when it shouldn't be."""
    if not source_text or not target_text:
        return False
    # If source is in protected terms or has identical non-English text, not untranslated
    if source_text in PROTECTED_TERMS or len(source_text.split()) <= 1:
        return False
    if source_text.strip().lower() != target_text.strip().lower():
        return False

    # Strip format placeholders, numbers, punctuation, and common international units
    cleaned = PLACEHOLDER.sub("", source_text)
    cleaned = re.sub(
        r"\b\d+(?:[.,]\d+)?\s*(?:ms|min|h|s|KB/s|MB/s|GB/s|GB|MB|KB|dp|px|%|slots?)\b",
        "",
        cleaned,
        flags=re.IGNORECASE,
    )
    cleaned = re.sub(r"[0-9\s.,/•·+:;!?()_\"'\\-]", "", cleaned)

    # If no substantive alphabetic words remain after removing placeholders/units/numbers, it's not untranslated
    if len(cleaned) < 3:
        return False

    return True


def translate_element(element: ET.Element, language: str) -> bool:
    """Translate an XML element (string, item, plurals, array)."""
    if element.get("translatable") == "false":
        return False

    changed = False

    if element.tag in ("string", "item"):
        if element.text and element.text.strip():
            result = translate(element.text, language)
            if result is not None:
                element.text = result
                changed = True
            else:
                return False

        for child in element:
            if child.tail and child.tail.strip():
                result = translate(child.tail, language)
                if result is not None:
                    child.tail = result
                    changed = True
                else:
                    return False

    elif element.tag in ("string-array", "array"):
        for item in element.findall("item"):
            if item.text and item.text.strip():
                result = translate(item.text, language)
                if result is not None:
                    item.text = result
                    changed = True
                else:
                    return False

    elif element.tag == "plurals":
        for item in element.findall("item"):
            if item.text and item.text.strip():
                result = translate(item.text, language)
                if result is not None:
                    item.text = result
                    changed = True
                else:
                    return False

    return changed


def validate_file(
    source_root: ET.Element,
    target_root: ET.Element,
    language: str,
    auto_heal: bool = True,
) -> tuple[int, int]:
    """Validate translation consistency, placeholders, and escaping against source."""
    source_map = {}
    for elem in source_root:
        key = resource_key(elem)
        if elem.text:
            source_map[key] = elem.text

    warnings = 0
    errors = 0

    for elem in target_root:
        key = resource_key(elem)
        tag, name = key
        lang_code = language

        if elem.text:
            orig = elem.text
            sanitized = sanitize_for_android_xml(orig, lang_code)
            if orig != sanitized:
                if auto_heal:
                    elem.text = sanitized
                warnings += 1

            # Validate format placeholders against source
            if key in source_map:
                src_ph = PLACEHOLDER.findall(source_map[key])
                tgt_ph = PLACEHOLDER.findall(elem.text)
                if sorted(src_ph) != sorted(tgt_ph):
                    print(
                        f"  [ERROR] {tag} '{name}': placeholder mismatch! "
                        f"Expected {src_ph}, found {tgt_ph}"
                    )
                    errors += 1

            # Check for leftover token strings
            if re.search(r"ZXQ|TOKEN\d|托肯|トークン", elem.text, re.IGNORECASE):
                print(f"  [ERROR] {tag} '{name}': token residue detected in '{elem.text}'")
                errors += 1

            # Check if multi-word string was left in English
            if key in source_map and is_likely_untranslated(source_map[key], elem.text, lang_code):
                print(f"  [WARN] {tag} '{name}': appears untranslated ('{elem.text[:40]}...')")
                warnings += 1

    return warnings, errors


def save_xml(root: ET.Element, destination: Path):
    """Write cleanly formatted Android XML with UTF-8 declaration."""
    tree = ET.ElementTree(root)
    ET.indent(tree, space="    ")
    tree.write(
        destination,
        encoding="utf-8",
        xml_declaration=True,
    )


def validate_all_translations():
    """Validate and auto-heal all existing strings.xml files without running MT server."""
    if not SOURCE.exists():
        raise SystemExit(f"English source not found: {SOURCE}")

    source_root = ET.parse(SOURCE).getroot()
    total_warnings = 0
    total_errors = 0

    print("\n--- Validating & Auto-Healing All Translations ---")
    for name, (language, folder) in LANGUAGES.items():
        destination = RES / folder / "strings.xml"
        if not destination.exists():
            print(f"\n{name} ({folder}): [SKIPPED - file does not exist]")
            continue

        print(f"\nValidating {name} ({destination.relative_to(ROOT)})...")
        tree = ET.parse(destination)
        root = tree.getroot()

        warns, errs = validate_file(source_root, root, language, auto_heal=True)
        total_warnings += warns
        total_errors += errs

        if warns > 0:
            save_xml(root, destination)
            print(f"  Saved with {warns} auto-healed issue(s).")
        else:
            print("  All strings clean and validated.")

    print("\n--------------------------------------------------")
    if total_errors == 0:
        print("RESULT: All translations are valid for Android Lint and Gradle builds.")
    else:
        print(f"RESULT: Found {total_errors} unresolved placeholder error(s). Please review logs.")


def main():
    parser = argparse.ArgumentParser(description="SmartIsland Translation Tool")
    parser.add_argument(
        "--validate-only",
        action="store_true",
        help="Validate and auto-heal all existing translations without running LibreTranslate",
    )
    parser.add_argument(
        "--reuse",
        action="store_true",
        help="Reuse existing translations and only translate missing or untranslated strings",
    )
    parser.add_argument(
        "--all",
        action="store_true",
        help="Regenerate all translations from scratch",
    )
    parser.add_argument(
        "--lang",
        type=str,
        default=None,
        help="Comma-separated list of language codes to process (e.g. es,pt,zh,ja)",
    )
    args = parser.parse_args()

    if args.validate_only:
        validate_all_translations()
        return

    choice = None
    if args.reuse:
        choice = "1"
    elif args.all:
        choice = "2"

    if not SOURCE.exists():
        raise SystemExit(f"English source not found: {SOURCE}")

    if choice is None:
        print("\n===== SmartIsland Translation Generator =====")
        print("1. Reuse existing translations and add missing/untranslated strings")
        print("2. Start from scratch and regenerate all translations")
        print("3. Validate and auto-heal existing translations (no server needed)")
        print("4. Exit")

        user_input = input("\nChoose (1/2/3/4): ").strip()
        if user_input == "4":
            return
        if user_input == "3":
            validate_all_translations()
            return
        if user_input not in ("1", "2"):
            raise SystemExit("Invalid choice.")
        choice = user_input

    # Check LibreTranslate server availability
    try:
        requests.get(API.replace("/translate", "/languages"), timeout=5).raise_for_status()
    except requests.RequestException:
        raise SystemExit(
            "LibreTranslate is not running at http://127.0.0.1:5000\n"
            "Run 'scripts\\run-translations.bat' or start LibreTranslate first,\n"
            "or use '--validate-only' to validate and fix existing translations."
        )

    target_languages = LANGUAGES
    if args.lang:
        selected_codes = {c.strip().lower() for c in args.lang.split(",")}
        target_languages = {
            k: v for k, v in LANGUAGES.items() if v[0] in selected_codes
        }
        if not target_languages:
            raise SystemExit(f"No matching languages for '{args.lang}'. Available: pt, es, zh, ja")

    source_root = ET.parse(SOURCE).getroot()
    total_resources = len(source_root)

    for name, (language, folder) in target_languages.items():
        destination = RES / folder / "strings.xml"
        destination.parent.mkdir(parents=True, exist_ok=True)

        existing = {}
        if destination.exists():
            existing_root = ET.parse(destination).getroot()
            existing = {
                resource_key(element): element
                for element in existing_root
            }

        output_root = ET.Element("resources")
        translated_count = 0
        reused_count = 0
        failed_count = 0

        print(f"\nProcessing {name} ({language}) -> {destination.relative_to(ROOT)}")

        for idx, source_element in enumerate(source_root, 1):
            key = resource_key(source_element)

            if source_element.get("translatable") == "false":
                output_root.append(copy.deepcopy(source_element))
                continue

            # In reuse mode (1), keep valid existing translations unless they are empty/untranslated
            if choice == "1" and key in existing:
                existing_elem = existing[key]
                # Auto-heal the existing element
                if existing_elem.text:
                    existing_elem.text = sanitize_for_android_xml(existing_elem.text, language)

                # Check if it was left untranslated in English from a previous run
                needs_translation = is_likely_untranslated(
                    source_element.text, existing_elem.text, language
                )

                if not needs_translation:
                    output_root.append(copy.deepcopy(existing_elem))
                    reused_count += 1
                    continue

            # Needs translation
            element = copy.deepcopy(source_element)
            success = translate_element(element, language)

            if success:
                output_root.append(element)
                translated_count += 1
            else:
                failed_count += 1
                # If translation failed, fallback to existing or source
                if key in existing:
                    output_root.append(copy.deepcopy(existing[key]))
                else:
                    output_root.append(copy.deepcopy(source_element))

            if idx % 100 == 0 or idx == total_resources:
                print(f"  Progress: {idx}/{total_resources} strings...")

        # Validate and auto-heal final XML
        validate_file(source_root, output_root, language, auto_heal=True)
        save_xml(output_root, destination)

        print(
            f"Saved {name}: {translated_count} translated, "
            f"{reused_count} reused, {failed_count} fallback(s)."
        )

    print("\nFinished! All translations generated and validated.")


if __name__ == "__main__":
    main()
