def call(String configFile = "prod.yaml") {
    def config = readYaml file: "config/${configFile}"

    pipeline {
        agent any

        environment {
            SLACK_CHANNEL = config.SLACK_CHANNEL_NAME
            ENVIRONMENT   = config.ENVIRONMENT
            CODE_PATH     = config.CODE_BASE_PATH
            MESSAGE       = config.ACTION_MESSAGE
        }

        stages {
            stage('Clone Repo') {
                steps {
                    git url: 'https://github.com/Rajnish9211/Tool.git', branch: 'main'
                }
            }

            stage('User Approval') {
                when {
                    expression { return config.KEEP_APPROVAL_STAGE }
                }
                steps {
                    timeout(time: 10, unit: 'MINUTES') {
                        input message: "Approve Ansible execution for environment: ${ENVIRONMENT}", ok: 'Approve'
                    }
                }
            }

            stage('Execute Ansible') {
                steps {
                    dir("${CODE_PATH}") {
                        sh 'ansible-playbook site.yml -i inventory.ini'
                    }
                }
            }

            stage('Slack Notification') {
                steps {
                    slackSend channel: "${SLACK_CHANNEL}", message: "${MESSAGE}"
                }
            }
        }
    }
}
