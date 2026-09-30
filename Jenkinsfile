pipeline {
  agent any

  parameters {
    string(name: 'POSTGRES_USER_PARAM', defaultValue: '', description: 'Optional DB username override')
    password(name: 'POSTGRES_PASSWORD_PARAM', defaultValue: '', description: 'Optional DB password override')
    password(name: 'FAUXNANCE_API_KEY_PARAM', defaultValue: '', description: 'Optional Fauxnance API key override')
  }

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  environment {
    DB_HOST = 'postgres'
    DB_PORT = '5432'
    POSTGRES_DB = 'ea-db'
    SPRINGBOOT_PORT = '8099'
    FAUXNANCE_API_URL = 'https://y4t9nq2bqf.execute-api.eu-west-2.amazonaws.com/v1'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Prepare Env') {
      steps {
        sh '''
          set -e

          POSTGRES_USER_EFFECTIVE="${POSTGRES_USER_PARAM:-${POSTGRES_USER:-eauser}}"
          POSTGRES_PASSWORD_EFFECTIVE="${POSTGRES_PASSWORD_PARAM:-${POSTGRES_PASSWORD:-securepassword}}"
          FAUXNANCE_API_KEY_EFFECTIVE="${FAUXNANCE_API_KEY_PARAM:-${FAUXNANCE_API_KEY:-test-key}}"

          if [ -z "${POSTGRES_USER_PARAM:-}" ] && [ -z "${POSTGRES_USER:-}" ]; then
            echo "POSTGRES_USER not provided; using default for CI"
          fi
          if [ -z "${POSTGRES_PASSWORD_PARAM:-}" ] && [ -z "${POSTGRES_PASSWORD:-}" ]; then
            echo "POSTGRES_PASSWORD not provided; using default for CI"
          fi
          if [ -z "${FAUXNANCE_API_KEY_PARAM:-}" ] && [ -z "${FAUXNANCE_API_KEY:-}" ]; then
            echo "FAUXNANCE_API_KEY not provided; using default test value"
          fi

          cat > .env <<EOF
POSTGRES_USER=${POSTGRES_USER_EFFECTIVE}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD_EFFECTIVE}
POSTGRES_DB=${POSTGRES_DB}
SPRINGBOOT_PORT=${SPRINGBOOT_PORT}
DB_HOST=${DB_HOST}
DB_PORT=${DB_PORT}
FAUXNANCE_API_URL=${FAUXNANCE_API_URL}
FAUXNANCE_API_KEY=${FAUXNANCE_API_KEY_EFFECTIVE}
EOF
        '''
      }
    }

    stage('Start Database') {
      steps {
        sh '''
          set -e

          if docker compose version >/dev/null 2>&1; then
            COMPOSE_CMD="docker compose"
          elif command -v docker-compose >/dev/null 2>&1; then
            COMPOSE_CMD="docker-compose"
          else
            echo "Neither 'docker compose' nor 'docker-compose' is available"
            exit 1
          fi

          $COMPOSE_CMD down -v --remove-orphans || true
          $COMPOSE_CMD up -d postgres

          for i in $(seq 1 60); do
            STATUS=$(docker inspect -f '{{.State.Health.Status}}' postgres-ea-trading 2>/dev/null || echo "starting")
            if [ "$STATUS" = "healthy" ]; then
              echo "Postgres is healthy"
              exit 0
            fi
            sleep 2
          done

          echo "Postgres did not become healthy in time"
          $COMPOSE_CMD logs postgres || true
          exit 1
        '''
      }
    }

    stage('Run All Tests') {
      steps {
        dir('api') {
          sh '''
            chmod +x mvnw || true
            ./mvnw -B -ntp clean verify
          '''
        }
      }
    }
  }

  post {
    always {
      junit testResults: 'api/target/surefire-reports/*.xml', allowEmptyResults: true
      archiveArtifacts artifacts: 'api/target/surefire-reports/*, api/target/failsafe-reports/*', allowEmptyArchive: true
      sh '''
        if docker compose version >/dev/null 2>&1; then
          docker compose down -v --remove-orphans || true
        elif command -v docker-compose >/dev/null 2>&1; then
          docker-compose down -v --remove-orphans || true
        fi
      '''
      cleanWs()
    }
  }
}
