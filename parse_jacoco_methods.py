import xml.etree.ElementTree as ET
import sys

try:
    tree = ET.parse('Backend/target/site/jacoco-aggregate/jacoco.xml')
    root = tree.getroot()
except Exception as e:
    print('Failed to parse jacoco.xml:', e)
    sys.exit(1)

target_classes = [
    'com/skillsync/auth/service/AuthService',
    'com/skillsync/auth/controller/AuthController',
    'com/skillsync/session/consumer/PaymentEventConsumer'
]

for clazz in root.iter('class'):
    c_name = clazz.get('name')
    if c_name in target_classes:
        print(f"\nClass: {c_name}")
        for method in clazz.findall('method'):
            m_name = method.get('name')
            m_desc = method.get('desc')
            m_missed = 0
            m_covered = 0
            for counter in method.findall('counter'):
                if counter.get('type') == 'BRANCH':
                    m_missed = int(counter.get('missed'))
                    m_covered = int(counter.get('covered'))
            if m_missed > 0:
                print(f"  Method: {m_name}{m_desc} - Missed branches: {m_missed}")
