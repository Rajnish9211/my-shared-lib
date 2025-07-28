def call(String configFile = "prod.yaml") {
    // Load YAML from shared library resources using libraryResource
    def yamlText = libraryResource("config/${configFile}")

    // ✅ Use Jenkins' pipelineUtilitySteps readYaml (from string)
    def config = readYaml text: yamlText

    def slackChannel = config.SLACK_CHANNEL_NAME
    def environmentName = config.ENVIRONMENT
    def codePath = config.CODE_BASE_PATH
    def message = config.ACTION_MESSAGE
    def approvalStage = config.KEEP_APPROVAL_STAGE

    pipeline {
        agent any

        stages {
            stage('Clone Repo') {
                steps {
                    git url: 'https://github.com/Rajnish9211/Tool.git', branch: 'main'
                }
            }

            stage('User Approval') {
                when {
                    expression { return approvalStage }
                }
                steps {
                    timeout(time: 10, unit: 'MINUTES') {
                        input message: "✅ Approve Ansible execution for environment: ${environmentName}", ok: 'Approve'
                    }
                }
            }

            stage('Run Ansible') {
                steps {
                    dir("${codePath}") {
                        sh "ansible-playbook site.yml -i inventory.ini"
                    }
                }
            }

            stage('Slack Notification') {
                steps {
                    slackSend channel: "${slackChannel}", message: "${message}"
                }
            }
        }
    }
}
