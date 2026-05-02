import xml.etree.ElementTree as ET
import glob
import sys

filter_service = sys.argv[1] if len(sys.argv) > 1 else None

print("UNCOVERED CLASSES:")
for xml_file in glob.glob('*/target/site/jacoco/jacoco.xml'):
    try:
        tree = ET.parse(xml_file)
        root = tree.getroot()
        module = xml_file.split('\\')[0] if '\\' in xml_file else xml_file.split('/')[0]
        
        if filter_service and filter_service not in module:
            continue

        for p in root.findall('package'):
            for c in p.findall('class'):
                # Check for BRANCH coverage
                branch_counter = c.find("counter[@type='BRANCH']")
                line_counter = c.find("counter[@type='LINE']")
                
                missed_branches = int(branch_counter.get('missed', 0)) if branch_counter is not None else 0
                covered_branches = int(branch_counter.get('covered', 0)) if branch_counter is not None else 0
                
                missed_lines = int(line_counter.get('missed', 0)) if line_counter is not None else 0
                covered_lines = int(line_counter.get('covered', 0)) if line_counter is not None else 0

                if missed_branches > 0 or (missed_lines > 0 and covered_lines == 0):
                    print(f"[{module}] {c.get('name')}: Branch({covered_branches}/{covered_branches+missed_branches}), Line({covered_lines}/{covered_lines+missed_lines})")
    except Exception as e:
        print(f"Error parsing {xml_file}: {e}")
