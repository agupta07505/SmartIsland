
import copy
import re
import time
import xml.etree.ElementTree as ET
from pathlib import Path

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

PLACEHOLDER = re.compile(
    r"%(?:\d+\$)?[-#+ 0,(<]*\d*(?:\.\d+)?[a-zA-Z%]"
)


def sanitize_for_android_xml(text):
    if not text:
        return text
    # Clean up stray backslashes that are not valid Android escapes (n, t, ', ", \\, @, ?, u)
    def clean_slash(match):
        nxt = match.group(1)
        if nxt in ("n", "t", "'", '"', "\\", "@", "?", "u"):
            return "\\" + nxt
        return nxt
    text = re.sub(r'\\(.)', clean_slash, text)
    # Ensure unescaped single quotes are escaped with \'
    # Match single quotes not preceded by backslash
    text = re.sub(r"(?<!\\)'", r"\'", text)
    return text


def translate(text, language):
    if not text or not text.strip():
        return text

    protected = {}

    # Unescape common Android XML escapes for translation accuracy
    cleaned_input = text.replace(r"\'", "'").replace(r'\"', '"').replace(r"\\", "\\")

    def replace(match):
        idx = len(protected)
        token = f"ZXQTOKEN{idx}QXZ"
        protected[idx] = (token, match.group())
        return token

    original = text
    cleaned_input = PLACEHOLDER.sub(replace, cleaned_input)

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
            result = response.json()["translatedText"]

            # Restore tokens flexibly handling case or spacing alterations by MT engines
            for idx, (token, value) in protected.items():
                # Direct match
                if token in result:
                    result = result.replace(token, value)
                else:
                    # Fuzzy / regex match for case-insensitive or spaced variants
                    pattern = re.compile(
                        rf"Z[XQ][Q]?[\s_]*(?:TOKEN|token|トークン|托肯)[\s_]*{idx}[\s_]*[QXZ]+",
                        re.IGNORECASE
                    )
                    result = pattern.sub(value, result)

            # Verification: all placeholders must be restored and no token residue
            token_residue = re.search(r"Z[XQ].*?Q[XZ]", result, re.IGNORECASE)
            all_restored = all(val in result for _, val in protected.values())
            if token_residue or not all_restored:
                return original

            return sanitize_for_android_xml(result)

        except requests.RequestException as error:
            if attempt == 2:
                print(f"Translation failed: {error}")
                return None
            time.sleep(2)

    return None


def resource_key(element):
    return element.tag, element.get("name", "")


def translate_element(element, language):
    if element.get("translatable") == "false":
        return False

    changed = False

    if element.tag in ("string", "item"):
        if element.text and element.text.strip():
            result = translate(element.text, language)
            if result is None:
                return False
            element.text = result
            changed = True

        for child in element:
            if child.tail and child.tail.strip():
                result = translate(child.tail, language)
                if result is None:
                    return False
                child.tail = result
                changed = True

    elif element.tag in ("string-array", "array"):
        for item in element.findall("item"):
            if item.text and item.text.strip():
                result = translate(item.text, language)
                if result is None:
                    return False
                item.text = result
                changed = True

    elif element.tag == "plurals":
        for item in element.findall("item"):
            if item.text and item.text.strip():
                result = translate(item.text, language)
                if result is None:
                    return False
                item.text = result
                changed = True

    return changed


def save_xml(root, destination):
    tree = ET.ElementTree(root)
    ET.indent(tree, space="    ")
    tree.write(
        destination,
        encoding="utf-8",
        xml_declaration=True,
    )


def main():
    if not SOURCE.exists():
        raise SystemExit(f"English source not found: {SOURCE}")

    try:
        requests.get(API.replace("/translate", "/languages"), timeout=5).raise_for_status()
    except requests.RequestException:
        raise SystemExit(
            "LibreTranslate is not running at http://127.0.0.1:5000"
        )

    print("\nSmartIsland Translation Generator")
    print("1. Reuse existing translations and add missing strings")
    print("2. Start from scratch and regenerate all translations")
    print("3. Exit")

    choice = input("\nChoose (1/2/3): ").strip()

    if choice == "3":
        return
    if choice not in ("1", "2"):
        raise SystemExit("Invalid choice.")

    source_root = ET.parse(SOURCE).getroot()

    for name, (language, folder) in LANGUAGES.items():
        destination = RES / folder / "strings.xml"
        destination.parent.mkdir(parents=True, exist_ok=True)

        existing = {}
        if choice == "1" and destination.exists():
            existing_root = ET.parse(destination).getroot()
            existing = {
                resource_key(element): element
                for element in existing_root
            }

        output_root = ET.Element("resources")
        failures = 0

        print(f"\n{name}: {destination.relative_to(ROOT)}")

        for source_element in source_root:
            key = resource_key(source_element)

            if source_element.get("translatable") == "false":
                output_root.append(copy.deepcopy(source_element))
                continue

            if choice == "1" and key in existing:
                output_root.append(copy.deepcopy(existing[key]))
                continue

            element = copy.deepcopy(source_element)

            if not translate_element(element, language):
                has_text = bool(
                    (element.text and element.text.strip())
                    or list(element)
                )
                if has_text:
                    failures += 1
                    if key in existing:
                        output_root.append(copy.deepcopy(existing[key]))
                    else:
                        output_root.append(copy.deepcopy(source_element))
                    continue

            output_root.append(element)

        if failures:
            print(f"Warning: {failures} resource(s) could not be translated.")

        save_xml(output_root, destination)
        print("Saved.")

    print("\nFinished. Review the generated XML before building.")


if __name__ == "__main__":
    main()
