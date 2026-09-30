pipeline {
  agent any

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
        withCredentials([
          usernamePassword(
            credentialsId: 'ea-postgres-creds',
            usernameVariable: 'POSTGRES_USER',
            passwordVariable: 'POSTGRES_PASSWORD'
          ),
          string(
            credentialsId: 'fauxnance-api-key',
            variable: 'FAUXNANCE_API_KEY'
          )
        ]) {
          sh '''
            cat > .env <<EOF
POSTGRES_USER=${POSTGRES_USER}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
POSTGRES_DB=${POSTGRES_DB}
SPRINGBOOT_PORT=${SPRINGBOOT_PORT}
DB_HOST=${DB_HOST}
DB_PORT=${DB_PORT}
FAUXNANCE_API_URL=${FAUXNANCE_API_URL}
FAUXNANCE_API_KEY=${FAUXNANCE_API_KEY}
EOF
          '''
        }
      }
    }

    stage('Start Database') {
      steps {
        sh '''
          docker compose down --volumes --remove-orphans || true
          docker compose up -d postgres

          for i in $(seq 1 60); do
            STATUS=$(docker inspect -f '{{.State.Health.Status}}' postgres-ea-trading 2>/dev/null || echo "starting")
            if [ "$STATUS" = "healthy" ]; then
              echo "Postgres is healthy"
              exit 0
            fi
            sleep 2
          done

          echo "Postgres did not become healthy in time"
          docker compose logs postgres || true
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
      sh 'docker compose down --volumes --remove-orphans || true'
      cleanWs()
    }
  }
}
