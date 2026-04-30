import xml.etree.ElementTree as ET
import os

def get_coverage(xml_path):
    try:
        tree = ET.parse(xml_path)
        root = tree.getroot()
        # JaCoCo XML structure: <report><counter type="LINE" missed="..." covered="..."/></report>
        # Counters are at the end of the report element
        counters = root.findall('counter')
        for counter in counters:
            if counter.get('type') == 'LINE':
                missed = int(counter.get('missed'))
                covered = int(counter.get('covered'))
                total = missed + covered
                if total == 0: return 0.0
                return (covered / total) * 100
    except Exception as e:
        return f"Error: {e}"
    return 0.0

backend_path = r'f:\Projects\SkillSync\Backend'
services = [
    'auth-service', 'user-service', 'notification-service', 'session-service', 
    'skill-service', 'payment-service', 'api-gateway', 'config-server', 'eureka-server',
    'skillsync-cache-common'
]

print(f"{'Service':<25} | {'Line Coverage (%)':<20}")
print("-" * 50)

total_covered = 0
total_lines = 0

for service in services:
    xml_path = os.path.join(backend_path, service, 'target', 'site', 'jacoco', 'jacoco.xml')
    if os.path.exists(xml_path):
        tree = ET.parse(xml_path)
        root = tree.getroot()
        for counter in root.findall('counter'):
            if counter.get('type') == 'LINE':
                missed = int(counter.get('missed'))
                covered = int(counter.get('covered'))
                line_total = missed + covered
                total_covered += covered
                total_lines += line_total
                coverage = (covered / line_total) * 100 if line_total > 0 else 0
                print(f"{service:<25} | {coverage:>18.2f}%")
    else:
        print(f"{service:<25} | {'N/A (Missing)':>18}")

if total_lines > 0:
    overall = (total_covered / total_lines) * 100
    print("-" * 50)
    print(f"{'OVERALL':<25} | {overall:>18.2f}%")
