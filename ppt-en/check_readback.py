import xml.etree.ElementTree as ET

path = r"N:\checkout\payment-gateway-challenge-java\.lark-slides\readback.xml"
tree = ET.parse(path)
root = tree.getroot()
ns = root.tag.split('}')[0] + '}' if '}' in root.tag else ''
slides = root.findall(ns + 'slide')
print("total slides:", len(slides))
for i, s in enumerate(slides, 1):
    sid = s.get('id', 'NO_ID')
    # first text content
    texts = []
    for shape in s.iter(ns + 'shape'):
        if shape.get('type') == 'text':
            for p in shape.iter(ns + 'p'):
                t = ''.join(p.itertext()).strip()
                if t:
                    texts.append(t)
                    break
        if texts:
            break
    print(f"{i:02d} | id={sid} | first_text={texts[0] if texts else ''}")
