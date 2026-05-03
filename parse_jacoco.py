import xml.etree.ElementTree as ET
import sys

try:
    tree = ET.parse('Backend/target/site/jacoco-aggregate/jacoco.xml')
    root = tree.getroot()
except Exception as e:
    print('Failed to parse jacoco.xml:', e)
    sys.exit(1)

missed_branches = []

for clazz in root.iter('class'):
    c_name = clazz.get('name')
    class_missed_branches = 0
    class_covered_branches = 0
    
    for counter in clazz.findall('counter'):
        if counter.get('type') == 'BRANCH':
            class_missed_branches = int(counter.get('missed'))
            class_covered_branches = int(counter.get('covered'))
            
    total_branches = class_missed_branches + class_covered_branches
    
    if class_missed_branches > 0:
        missed_branches.append({
            'class': c_name,
            'missed': class_missed_branches,
            'covered': class_covered_branches,
            'total_branches': total_branches
        })

sorted_classes = sorted(missed_branches, key=lambda x: x['missed'], reverse=True)
for b in sorted_classes[:10]:
    coverage_pct = (b['covered'] / b['total_branches']) * 100 if b['total_branches'] > 0 else 100
    print(f"{b['class']}: {b['missed']} missed branches, coverage: {coverage_pct:.2f}%, total: {b['total_branches']}")
