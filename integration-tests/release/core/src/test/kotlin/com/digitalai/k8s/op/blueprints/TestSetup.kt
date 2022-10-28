package com.digitalai.k8s.op.blueprints

data class TestSetup(
    val desscription: String,
    val customAnswers: Map<String, String>,
    val validateCrValues: Map<String, String>,
)