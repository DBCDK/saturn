#!groovy

def workerNode = "devel11"

pipeline {
	agent {label workerNode}
	tools {
		// refers to the name set in manage jenkins -> global tool configuration
		jdk 'jdk11'
		maven "Maven 3"
	}
	environment {
		DOCKER_TAG = "${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
		GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
	}
	triggers {
		upstream(upstreamProjects: "Docker-payara5-bump-trigger",
			threshold: hudson.model.Result.SUCCESS)
		pollSCM("H/03 * * * *")
	}
	options {
		timestamps()
	}
	stages {
		stage("docker build saturn-passwordstoresync") {
			steps {
				script {
					sh """
						passwordstore/build
					"""
				}
			}
		}
		stage("verify") {
			steps {
				sh "mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent verify pmd:pmd"
				junit "**/target/surefire-reports/TEST-*.xml,**/target/failsafe-reports/TEST-*.xml"
			}
		}
		stage("warnings") {
			steps {
				warnings consoleParsers: [
					[parserName: "Java Compiler (javac)"],
					[parserName: "JavaDoc Tool"]],
					unstableTotalAll: "0",
					failedTotalAll: "0"
			}
		}
		stage("pmd") {
			steps {
				step([$class: 'hudson.plugins.pmd.PmdPublisher',
					  pattern: '**/target/pmd.xml',
					  unstableTotalAll: "0",
					  failedTotalAll: "0"])
			}
		}
		stage("docker push") {
            when {
                branch "master"
            }
            steps {
                script {
                    sh """
                        docker push docker-metascrum.artifacts.dbccloud.dk/saturn-service:${BRANCH_NAME}-${BUILD_NUMBER}
                    """
                }
            }
        }
		stage("docker push saturn-passwordstoresync") {
			when {
				branch "master"
			}
			steps {
				script {
					sh """
						docker push docker-metascrum.artifacts.dbccloud.dk/saturn-passwordstoresync:${env.BRANCH_NAME}-${env.BUILD_NUMBER}
					"""
				}
			}
		}
		stage("update staging version") {
			agent {
				docker {
					label workerNode
					image "docker-dbc.artifacts.dbccloud.dk/build-env"
					alwaysPull true
				}
			}
			when {
				branch "master"
			}
			steps {
				dir("deploy") {
					sh """
						set-new-version dataio-saturn-service.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/saturn-deploy ${env.DOCKER_TAG} -b staging
					"""
				}
			}
		}
	}
}
