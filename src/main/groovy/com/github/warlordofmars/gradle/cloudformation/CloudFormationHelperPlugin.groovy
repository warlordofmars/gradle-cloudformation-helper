package com.github.warlordofmars.gradle.cloudformation

import org.gradle.api.Plugin
import org.gradle.api.Project

import groovy.json.JsonSlurper
import java.io.ByteArrayOutputStream


class CloudFormationHelperPlugin implements Plugin<Project> {

    void apply(Project project) {
        

        project.plugins.apply('jp.classmethod.aws.cloudformation')


        project.ext.tests = [
            cfnTemplateDeployedTest: [project.cloudformationSource, 'Resume Website CloudFormation Stack Created / Updated Successfully '],
            cfnTemplateDeployedProdTest: [project.cloudformationSource, 'Resume Website CloudFormation Stack Created / Updated Successfully in Production'],
            cfnLintTest: [project.cloudformationSource, 'Resume Website CloudFormation Linting / Syntax Check'],
            cfnNagTest: [project.cloudformationSource, 'Resume Website CloudFormation Best Practices'],
        ]



        project.task('deploy') {
            description 'Deploy CloudFormation Template for Resume Website'
            dependsOn project.awsCfnMigrateStackAndWaitCompleted, project.rootProject.registerTests
            
            doLast {

                def out = new ByteArrayOutputStream()
                project.exec {
                    commandLine 'aws', 'cloudformation', 'describe-stacks', '--stack-name', project.cloudFormation.stackName
                    standardOutput out
                }
                
                def stackDetails = new JsonSlurper().parseText(out.toString())['Stacks'][0]
                

                def message = "cloudformation stack \"${stackDetails['StackName']}\" has status: \"${stackDetails['StackStatus']}\"\n\nstack created: \"${stackDetails['CreationTime']}\"\nstack last updated: \"${stackDetails['LastUpdatedTime']}\"\n\n"
                stackDetails['Outputs'].each { output ->
                    message = message + "${output['OutputKey']}: \"${output['OutputValue']}\"\n"
                }

                def cfnTemplateDeployedTestObj = project.rootProject.cfnTemplateDeployedTest

                if(System.env.containsKey('PROMOTE')) {
                    cfnTemplateDeployedTestObj = project.rootProject.cfnTemplateDeployedProdTest
                }

                if(stackDetails['StackStatus'] in ["CREATE_COMPLETE", "UPDATE_COMPLETE"]) {
                    cfnTemplateDeployedTestObj.success(message)
                } else {
                    cfnTemplateDeployedTestObj.failure('CloudFormation Failed to Deploy', message)
                }
                
            }
        }

        project.awsCfnUploadTemplate.finalizedBy project.deploy
        project.awsCfnMigrateStack.finalizedBy project.deploy
        project.awsCfnWaitStackComplete.finalizedBy project.deploy
        project.awsCfnMigrateStackAndWaitCompleted.finalizedBy project.deploy

        project.task('delete') {
            dependsOn project.awsCfnDeleteStackAndWaitCompleted
        }

        project.awsCfnMigrateStack.dependsOn project.awsCfnUploadTemplate

        project.task('cfnLint') {
            dependsOn project.rootProject.checkPrerequisites, project.rootProject.registerTests
            
            doFirst {
                def out = new ByteArrayOutputStream()
                def result = project.exec {
                    commandLine 'cfn-lint', project.cloudformationSource
                    standardOutput out
                    ignoreExitValue true
                }
                if(result.getExitValue() == 0) {
                    project.rootProject.cfnLintTest.success('All Checks Passed')
                } else {
                    project.rootProject.cfnLintTest.failure('CloudFormation Syntax Error', out.toString())
                }
            }
        }

        project.task('cfnNag') {
            dependsOn project.rootProject.checkPrerequisites, project.rootProject.registerTests
            
            doFirst {
                def out = new ByteArrayOutputStream()
                def result = project.exec {
                    commandLine 'cfn_nag_scan', '--input-path', project.cloudformationSource
                    standardOutput out
                    ignoreExitValue true
                }
                if(result.getExitValue() == 0) {
                    project.rootProject.cfnNagTest.success(out.toString())
                } else {
                    project.rootProject.cfnNagTest.failure('Not Adhering to CloudFormation Best Practices', out.toString())
                }
                
            }
        }

        project.task('build') {
            dependsOn project.cfnLint, project.cfnNag
        }

    }

}