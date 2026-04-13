export type LearnDocFormat = 'markdown' | 'html' | 'docx';

export interface LearnDocItem {
  id: string;
  fileName: string;
  title: string;
  format: LearnDocFormat;
  category: LearnDocCategory;
}

export const LEARN_DOC_CATEGORIES = [
  'Architecture & Flows',
  'Service Deep Dives',
  'Security & Identity',
  'Data & Messaging',
  'Infrastructure & Delivery',
  'Observability',
  'Testing & Reliability',
  'General',
] as const;

export type LearnDocCategory = (typeof LEARN_DOC_CATEGORIES)[number];

const DOC_FILE_NAMES = [
  'actual_security_analysis_audit.md',
  'API_GATEWAY_ARCHITECTURE_DEEP_DIVE.md',
  'architectural_deep_dive_request_lifecycle.md',
  'AUTH_SERVICE_DEEP_DIVE.md',
  'AWS_CLOUD_ARCHITECTURE_DEEP_DIVE.md',
  'CICD_ARCHITECTURE_DEEP_DIVE.md',
  'complete_authentication_authorization_master_guide.md',
  'complete_security_request_lifecycle.md',
  'comprehensive_security_architecture_analysis.md',
  'comprehensive_storage_and_persistence_audit.md',
  'CONFIG_SERVER_DEEP_DIVE.md',
  'definitive_skillsync_architectural_breakdown.md',
  'DOCKER_CONTAINERIZATION_DEEP_DIVE.md',
  'EUREKA_SERVICE_DISCOVERY_DEEP_DIVE.md',
  'EXCEPTION_HANDLING_DEEP_DIVE.md',
  'FEIGN_CLIENT_DEEP_DIVE.md',
  'GIT_GITHUB_DEEP_DIVE.md',
  'GRAFANA_MONITORING_DEEP_DIVE.md',
  'jwt_and_oauth_master_guide.md',
  'JWT_OAuth_MasterGuide.docx',
  'NGINX_ARCHITECTURE_DEEP_DIVE.md',
  'NOTIFICATION_SERVICE_ARCHITECTURE_DEEP_DIVE.md',
  'PAYMENT_SERVICE_ARCHITECTURE_DEEP_DIVE.md',
  'PROJECT_STRUCTURE_DEEP_DIVE.md',
  'RABBITMQ_DEEP_DIVE.md',
  'REDIS_INTEGRATION_DEEP_DIVE.md',
  'secret_keys_and_encryption_analysis.md',
  'SkillSync_Architecture_Deep_Dive.docx',
  'skillsync_end_to_end_trace_analysis.md',
  'skillsync_master_security_document.html',
  'skillsync_visual_flow_diagrams.html',
  'skillsync_visual_flow_diagrams_part2.html',
  'skillsync_visual_flow_diagrams_part3.html',
  'SMTP_EMAIL_ARCHITECTURE_DEEP_DIVE.md',
  'stateless_vs_stateful_architectural_deep_dive.md',
  'SWAGGER_OPENAPI_DEEP_DIVE.md',
  'SYSTEM_ARCHITECTURE_DEEP_DIVE.md',
  'TESTING_ARCHITECTURE_DEEP_DIVE.md',
  'USER_SERVICE_ARCHITECTURE_DEEP_DIVE.md',
  'WEBSOCKET_REALTIME_DEEP_DIVE.md',
  'ZIPKIN_DISTRIBUTED_TRACING_DEEP_DIVE.md',
] as const;

const WORD_OVERRIDES: Record<string, string> = {
  api: 'API',
  auth: 'Auth',
  aws: 'AWS',
  cicd: 'CI/CD',
  config: 'Config',
  deep: 'Deep',
  dive: 'Dive',
  docx: 'DOCX',
  docker: 'Docker',
  eureka: 'Eureka',
  feign: 'Feign',
  gateway: 'Gateway',
  github: 'GitHub',
  grafana: 'Grafana',
  jwt: 'JWT',
  nginx: 'NGINX',
  oauth: 'OAuth',
  rabbitmq: 'RabbitMQ',
  redis: 'Redis',
  skillsync: 'SkillSync',
  smtp: 'SMTP',
  swagger: 'Swagger',
  websocket: 'WebSocket',
  zipkin: 'Zipkin',
};

const LOWERCASE_WORDS = new Set(['and', 'or', 'of', 'to', 'vs', 'in', 'for', 'the']);

function detectFormat(fileName: string): LearnDocFormat {
  if (fileName.endsWith('.html')) {
    return 'html';
  }

  if (fileName.endsWith('.docx')) {
    return 'docx';
  }

  return 'markdown';
}

function detectCategory(fileName: string): LearnDocCategory {
  const name = fileName.toLowerCase();

  if (/(auth|oauth|jwt|security|secret|encryption)/.test(name)) {
    return 'Security & Identity';
  }

  if (/(config_server|eureka|gateway|notification_service|payment_service|user_service|nginx|swagger|feign|smtp)/.test(name)) {
    return 'Service Deep Dives';
  }

  if (/(redis|rabbitmq|storage|persistence)/.test(name)) {
    return 'Data & Messaging';
  }

  if (/(monitoring|grafana|zipkin|trace)/.test(name)) {
    return 'Observability';
  }

  if (/(testing|exception)/.test(name)) {
    return 'Testing & Reliability';
  }

  if (/(docker|aws|cicd|git_github)/.test(name)) {
    return 'Infrastructure & Delivery';
  }

  if (/(architecture|architectural|project_structure|request_lifecycle|visual_flow|stateless_vs_stateful|system_)/.test(name)) {
    return 'Architecture & Flows';
  }

  return 'General';
}

function humanizeTitle(fileName: string): string {
  const withoutExtension = fileName.replace(/\.[a-z0-9]+$/i, '');
  const words = withoutExtension.replace(/_/g, ' ').split(/\s+/).filter(Boolean);

  return words
    .map((word, index) => {
      const lowerWord = word.toLowerCase();
      const override = WORD_OVERRIDES[lowerWord];

      if (override) {
        return override;
      }

      if (index > 0 && LOWERCASE_WORDS.has(lowerWord)) {
        return lowerWord;
      }

      return lowerWord.charAt(0).toUpperCase() + lowerWord.slice(1);
    })
    .join(' ');
}

export const LEARN_DOCS: LearnDocItem[] = DOC_FILE_NAMES.map((fileName, index) => ({
  id: `doc-${index + 1}`,
  fileName,
  title: humanizeTitle(fileName),
  format: detectFormat(fileName),
  category: detectCategory(fileName),
}));
