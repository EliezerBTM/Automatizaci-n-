// QA practice INF-433 (Unit II-2): Jenkins pipeline for Spring PetClinic.
// Stages: checkout -> build -> JUnit (unit) -> Selenium (UI) -> publish evidence.
// Works on Linux agents (sh) and Windows agents (bat). Requirements on the agent:
// JDK 17+, Chrome or Chromium installed and reachable (set CHROME_BIN if it is not on
// the PATH) and Internet access so Maven and Selenium Manager can download what they need.

// The project enforces code format (spring-javaformat) and "nohttp" checks in the
// validate phase. They are skipped here so the QA pipeline only reports test results;
// run "./mvnw spring-javaformat:apply" locally to keep the sources formatted.
def MVN_COMMON = '-B -Dspring-javaformat.skip=true -Dcheckstyle.skip=true'

def mvn(String arguments) {
    if (isUnix()) {
        sh "./mvnw ${arguments}"
    } else {
        bat "mvnw.cmd ${arguments}"
    }
}

pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage('Codigo') {
            steps {
                // Gets the exact revision that is going to be tested.
                checkout scm
                script {
                    if (isUnix()) {
                        sh 'git log -1 --format="Commit sometido a prueba: %H %s"'
                    } else {
                        bat 'git log -1 --format="Commit sometido a prueba: %%H %%s"'
                    }
                }
            }
        }

        stage('Entorno') {
            steps {
                // Records the tool versions used by this run (needed for the report).
                script {
                    if (isUnix()) {
                        sh 'java -version; ./mvnw -v; uname -a; (google-chrome --version || chromium --version || chromium-browser --version || true)'
                    } else {
                        bat 'java -version & mvnw.cmd -v & ver'
                    }
                }
            }
        }

        stage('Compilar') {
            steps {
                // Resolves dependencies and compiles main and test sources (no tests yet).
                script { mvn("${MVN_COMMON} -DskipTests package") }
            }
        }

        stage('JUnit') {
            steps {
                // Unit suite: pure business-rule tests, no browser and no database.
                script { mvn("${MVN_COMMON} test -Dtest=PetValidatorRulesTest") }
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/TEST-*PetValidatorRulesTest.xml', allowEmptyResults: false
                }
            }
        }

        stage('Selenium') {
            steps {
                // UI suite: Spring Boot starts the app on a random port and Chrome drives it.
                // The "selenium" tag is excluded by default in the pom; -Dexcluded.groups=none lifts that.
                script {
                    mvn("${MVN_COMMON} test -Dtest=OwnerFlowSeleniumTest -Dexcluded.groups=none -Dselenium.headless=true")
                }
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/TEST-*OwnerFlowSeleniumTest.xml', allowEmptyResults: false
                }
            }
        }
    }

    post {
        always {
            // Evidence: screenshots per step plus the raw Surefire reports of both suites.
            archiveArtifacts artifacts: 'target/screenshots/**/*.png', allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/surefire-reports/**', allowEmptyArchive: true
        }
        success {
            echo 'Pipeline OK: JUnit y Selenium aprobados.'
        }
        failure {
            echo 'Pipeline con fallos: revisar el reporte JUnit y las capturas archivadas.'
        }
    }
}
