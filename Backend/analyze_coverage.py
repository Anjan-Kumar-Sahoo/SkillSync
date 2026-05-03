import xml.etree.ElementTree as ET
import os
import glob
import fnmatch

backend_path = r'f:\Projects\SkillSync\Backend'
services = [
    'auth-service', 'user-service', 'notification-service', 'session-service', 
    'skill-service', 'payment-service', 'api-gateway', 'config-server', 'eureka-server',
    'skillsync-cache-common'
]

exclusions = [
    '**/dto/**',
    '**/entity/**',
    '**/enums/**',
    '**/mapper/**',
    '**/repository/**',
    '**/*Exception.java',
    '**/config/**',
    '**/*Application.java',
    '**/*Config.java',
    '**/eureka/**',
    '**/discovery/**',
    '**/gateway/config/**',
    '**/websocket/config/**',
    '**/*Properties.java',
    '**/*Constants.java'
]

# Convert java path pattern to package/class pattern
xml_exclusions = [
    '*/dto/*', '**/dto/*', '*dto*',
    '*/entity/*', '**/entity/*', '*entity*',
    '*/enums/*', '**/enums/*', '*enums*',
    '*/mapper/*', '**/mapper/*', '*mapper*',
    '*/repository/*', '**/repository/*', '*repository*',
    '*Exception',
    '*/config/*', '**/config/*', '*config*',
    '*Application',
    '*Config',
    '*/eureka/*', '**/eureka/*', '*eureka*',
    '*/discovery/*', '**/discovery/*', '*discovery*',
    '*/gateway/config/*', '**/gateway/config/*',
    '*/websocket/config/*', '**/websocket/config/*',
    '*Properties',
    '*Constants'
]

def is_excluded(class_name):
    # class_name is like com/skillsync/auth/dto/AuthResponse
    for ex in xml_exclusions:
        if fnmatch.fnmatch(class_name, ex):
            return True
    return False

print(f"{'Service':<25} | {'Before Exclusions':<20} | {'After Exclusions':<20}")
print("-" * 75)

total_covered_before = 0
total_lines_before = 0

total_covered_after = 0
total_lines_after = 0

for service in services:
    xml_path = os.path.join(backend_path, service, 'target', 'site', 'jacoco', 'jacoco.xml')
    if os.path.exists(xml_path):
        tree = ET.parse(xml_path)
        root = tree.getroot()
        
        svc_covered_before = 0
        svc_lines_before = 0
        svc_covered_after = 0
        svc_lines_after = 0

        for pkg in root.findall('package'):
            for cls in pkg.findall('class'):
                class_name = cls.get('name')
                
                cls_covered = 0
                cls_missed = 0
                for counter in cls.findall('counter'):
                    if counter.get('type') == 'LINE':
                        cls_missed = int(counter.get('missed'))
                        cls_covered = int(counter.get('covered'))
                
                cls_total = cls_missed + cls_covered
                
                # Before
                svc_covered_before += cls_covered
                svc_lines_before += cls_total
                total_covered_before += cls_covered
                total_lines_before += cls_total
                
                # After
                if not is_excluded(class_name):
                    svc_covered_after += cls_covered
                    svc_lines_after += cls_total
                    total_covered_after += cls_covered
                    total_lines_after += cls_total

        cov_before_pct = (svc_covered_before / svc_lines_before) * 100 if svc_lines_before > 0 else 0
        cov_after_pct = (svc_covered_after / svc_lines_after) * 100 if svc_lines_after > 0 else 0
        
        print(f"{service:<25} | {cov_before_pct:>18.2f}% | {cov_after_pct:>18.2f}%")
    else:
        print(f"{service:<25} | {'N/A':>18} | {'N/A':>18}")

if total_lines_before > 0:
    overall_before = (total_covered_before / total_lines_before) * 100
    overall_after = (total_covered_after / total_lines_after) * 100
    print("-" * 75)
    print(f"{'OVERALL':<25} | {overall_before:>18.2f}% | {overall_after:>18.2f}%")
