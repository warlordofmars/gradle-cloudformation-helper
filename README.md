# gradle-cloudformation-helper

[![latest jitpack release](https://jitpack.io/v/warlordofmars/gradle-cloudformation-helper.svg)](https://jitpack.io/#warlordofmars/gradle-cloudformation-helper)

## Overview

Gradle plugin to provide full automation for a CloudFormation project.  This includes static analysis of the CloudFormation templates themselves as well as automation to deploy a stack from a template

## Features

* **Syntax Validation** - CloudFormation template syntax is checked against CloudFormation specifications.  All resources defined are check to make sure the match what CloudFormation is expecting.
* **Best Practices Check** - CloudFormation template is analyzed for any deviation from CloudFormation best practices
* **Stack Deployment** -CloudFormation stack deployed from CloudFormation template
* **Test Results** - All tests that are performed throughout the `build` and `deploy` process are captured and recorded in a JUnit-style XML report.


## Prerequisites

There are two prerequisites required to exist prior to using this plugin:

### cfn-lint

[cfn-lint](https://github.com/awslabs/cfn-python-lint) is used for basic linting and sytax validation of CloudFormation templates.

To install `cfn-lint` utility, run the following:

```bash
pip install cfn-lint
```

### cfn_nag

[cfn_nag](https://github.com/stelligent/cfn_nag) is used for CloudFormation template static analysis to ensure templates are adhering to best practices.

To install `cfn_nag` utility, run the following:

```bash
gem install cfn-nag
```

## Setup

To use this plugin, the following buildscript repositories and dependencies must be configured:

```gradle
buildscript {
  repositories {
    maven { url 'https://jitpack.io' }
  }
  dependencies {
    classpath 'com.github.warlordofmars:gradle-cloudformation-helper:release-0.1.8'
  }
}
```

Then to apply the plugin:

```gradle
apply plugin: 'com.github.warlordofmars.gradle.cloudformation'
```

To configure:

```gradle
cloudformation {

    // the name of the cloudformation stack
    stackName = '<some_stack_name>'

    // the cloudformation template file
    templateFile = file(rootProject.cloudformationSource)
    
    // an existing S3 bucket that can be used to host the cloudformation template
    templateBucket = rootProject.cloudformationBucket
    
    // a mapping of stack parameters and their values to be used
    stackParams = [
        ParamName: '<Some_Param_Value>',
        AnotherParamName: '<Some_Other_Param_Value>'
    ]
    
    // is this build part of promote step?
    isPromote = System.env.containsKey('PROMOTE)

}
```

## Versioning

Versioning on this project is applied automatically on all changes using the [axion-release-plugin](https://github.com/allegro/axion-release-plugin).  Git tags are created for all released versions, and all available released versions can be viewed in the [Releases](https://github.com/warlordofmars/gradle-cloudformation-helper/releases) section of this project.

## Author

* **John Carter** - [warlordofmars](https://github.com/warlordofmars)

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* Using the [gradle-aws-plugin](https://github.com/classmethod/gradle-aws-plugin) for AWS API interactions (create / update / delete CloudFormation stacks)
* Using [cfn_nag](https://github.com/stelligent/cfn_nag) for CloudFormation template static analysis to ensure templates are adhering to best practices
* Using [cfn-lint](https://github.com/awslabs/cfn-python-lint) for basic linting and sytax validation of CloudFormation templates
