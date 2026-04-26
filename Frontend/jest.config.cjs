module.exports = {
  testEnvironment: 'jsdom',
  roots: ['<rootDir>/src', '<rootDir>/tests'],
  setupFiles: ['<rootDir>/tests/polyfills.ts'],
  setupFilesAfterEnv: ['<rootDir>/tests/setupTests.ts'],
  transform: {
    '^.+\\.(t|j)sx?$|^.+\\.(m|c)js$': [
      '@swc/jest',
      {
        jsc: {
          target: 'es2022',
          parser: {
            syntax: 'typescript',
            tsx: true,
            decorators: false,
          },
          transform: {
            react: {
              runtime: 'automatic',
            },
            optimizer: {
              globals: {
                vars: {
                  'import.meta.env.PROD': 'process.env.JEST_IMPORT_META_PROD === "true"',
                  'import.meta.env.DEV': 'process.env.JEST_IMPORT_META_PROD !== "true"',
                  'import.meta.env.VITE_API_URL': 'process.env.JEST_IMPORT_META_VITE_API_URL',
                  'import.meta.env.VITE_GOOGLE_CLIENT_ID': '"test-google-client-id"',
                  'import.meta.env.VITE_BACKEND_BASE_URL': '"http://localhost:8080"',
                  'import.meta.env.VITE_MONITORING_BASE_URL': '"http://localhost:8080"',
                  'import.meta.env.VITE_SWAGGER_URL': '"http://localhost:8080/swagger-ui"',
                  'import.meta.env.VITE_EUREKA_URL': '"http://localhost:8761"',
                  'import.meta.env.VITE_RABBITMQ_URL': '"http://localhost:15672"',
                  'import.meta.env.VITE_PROMETHEUS_URL': '"http://localhost:9090"',
                  'import.meta.env.VITE_GRAFANA_URL': '"http://localhost:3000"',
                  'import.meta.env.VITE_LOKI_READY_URL': '"http://localhost:3100/ready"',
                  'import.meta.env.VITE_ZIPKIN_URL': '"http://localhost:9411"',
                  'import.meta.env.VITE_SONAR_URL': '"http://localhost:9000"'
                }
              }
            }
          },
        },
        module: {
          type: 'commonjs',
        },
      },
    ],
  },
  moduleNameMapper: {
    '^.+\\.(css|less|scss|sass)$': 'identity-obj-proxy',
    '^.+\\.(png|jpg|jpeg|gif|webp|svg)$': '<rootDir>/tests/mocks/fileMock.ts',
  },
  testPathIgnorePatterns: ['/node_modules/', '/dist/'],
  transformIgnorePatterns: [],
  collectCoverageFrom: [
    'src/App.tsx',
    'src/components/**/*.{ts,tsx}',
    'src/context/**/*.{ts,tsx}',
    'src/hooks/**/*.{ts,tsx}',
    'src/routes/**/*.{ts,tsx}',
    'src/services/**/*.{ts,tsx}',
    'src/store/**/*.{ts,tsx}',
    'src/utils/**/*.{ts,tsx}',
    '!src/main.tsx',
    '!src/types/**/*.d.ts',
    '!src/**/*.css',
  ],
  coverageDirectory: '<rootDir>/coverage',
  coverageReporters: ['text', 'lcov', 'html'],
  coverageThreshold: {
    global: {
      branches: 0,
      functions: 0,
      lines: 0,
      statements: 0,
    },
  },
};
