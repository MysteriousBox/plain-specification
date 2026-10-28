// plain-specification 验证流水线：只允许 dev，只跑 mvn verify，不 deploy。
// pom 里有 OSSRH distributionManagement 与 release profile（GPG 绑定 verify），
// 这里不激活 -Prelease，也不会执行 deploy。

def isDevBranch(String name) {
    return name == 'dev' || name == 'origin/dev'
}

pipeline {
    agent {
        label 'non-k3s-agent'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    stages {
        stage('Validate branch') {
            steps {
                script {
                    def current = env.BRANCH_NAME ?: env.GIT_BRANCH ?: ''
                    if (!isDevBranch(current)) {
                        error("本流水线只允许 dev 分支，当前：${current ?: '未知'}")
                    }
                }
            }
        }

        stage('Validate toolchain') {
            steps {
                sh '''
                    command -v mvn >/dev/null 2>&1 || { echo "Maven is not available on this agent." >&2; exit 1; }
                    mvn -version
                '''
            }
        }

        stage('Verify') {
            steps {
                configFileProvider([configFile(fileId: '26b3b732-7088-484a-9fd4-2d481aaf630b', variable: 'MAVEN_SETTINGS')]) {
                    sh 'mvn -B -s "$MAVEN_SETTINGS" clean verify'
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            archiveArtifacts artifacts: '**/target/site/jacoco/**', allowEmptyArchive: true
        }
        failure {
            echo '验证失败：修复后重新触发；本流水线不会发布任何构件。'
        }
    }
}
