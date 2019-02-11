package com.github.warlordofmars.gradle.cloudformation

import org.gradle.api.Plugin
import org.gradle.api.Project

import groovy.json.JsonSlurper
import java.io.ByteArrayOutputStream


class CloudFormationHelperPlugin implements Plugin<Project> {

    CloudFormationExtension mExtension

    void apply(Project project) {
        
        mExtension = project.extensions.create('cloudformation', CloudFormationExtension)

        project.plugins.apply('jp.classmethod.aws.cloudformation')
        project.plugins.apply('com.github.warlordofmars.gradle.prerequisites')

        project.rootProject.plugins.apply('com.github.warlordofmars.gradle.customtest')

        
        project.ext.tests = [
            cfnTemplateDeployedTest: [project.cloudFormation.templateFile, 'CloudFormation Stack Created / Updated Successfully '],
            cfnTemplateDeployedProdTest: [project.cloudFormation.templateFile, 'CloudFormation Stack Created / Updated Successfully in Production'],
            cfnLintTest: [project.cloudFormation.templateFile, 'CloudFormation Linting / Syntax Check'],
            cfnNagTest: [project.cloudFormation.templateFile, 'CloudFormation Best Practices'],
        ]

        project.ext.prerequisites << [
            'aws': 'Install via \'brew install awscli\'',
            'cfn-lint': 'Install via \'pip install cfn-lint\'',
            'cfn_nag_scan': 'Install via \'gem install cfn-nag\'',
        ]
        
        project.afterEvaluate {
            project.cloudFormation {
                stackName mExtension.stackName
                templateFile mExtension.templateFile
                templateBucket mExtension.templateBucket
                templateKeyPrefix mExtension.stackName
                stackParams (mExtension.stackParams)
            }
        }
        
        def TASK_GROUP = 'CloudFormation'

        project.afterEvaluate {
            project.task('deploy') {
                description 'Deploy CloudFormation Stack from Template'
                group TASK_GROUP
                dependsOn project.awsCfnMigrateStackAndWaitCompleted, project.rootProject.registerTests, project.checkPrerequisites
                
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

                    if(mExtension.isPromote) {
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

        }

        project.task('delete') {
            description 'Delete CloudFormation Stack'
            group TASK_GROUP
            dependsOn project.awsCfnDeleteStackAndWaitCompleted
        }

        project.awsCfnMigrateStack.dependsOn project.awsCfnUploadTemplate

        project.task('cfnLint') {
            description 'Run cfn-lint syntax check against CloudFormation template'
            group TASK_GROUP
            dependsOn project.checkPrerequisites, project.rootProject.registerTests
            
            doFirst {
                def out = new ByteArrayOutputStream()
                def result = project.exec {
                    commandLine 'cfn-lint', project.cloudFormation.templateFile
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
            description 'Run cfn_nag_scan best practices check against CloudFormation template'
            group TASK_GROUP
            dependsOn project.checkPrerequisites, project.rootProject.registerTests
            
            doFirst {
                def out = new ByteArrayOutputStream()
                def result = project.exec {
                    commandLine 'cfn_nag_scan', '--input-path', project.cloudFormation.templateFile
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
            description 'Meta task to perform all required tasks as part of a CloudFormation "build"'
            group TASK_GROUP
            dependsOn project.cfnLint, project.cfnNag
        }

    }

}