package com.github.warlordofmars.gradle.cloudformation

class CloudFormationExtension {
    String stackName
    File templateFile
    String templateBucket
    Map stackParams
    boolean isPromote

}
