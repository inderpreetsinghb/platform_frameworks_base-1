/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.platform.coverage

import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.MethodItem
import com.android.tools.metalava.model.text.ApiFile
import java.io.File
import java.io.FileWriter

/** Usage: extract-flagged-apis <api text file> <output .pb file> */
fun main(args: Array<String>) {
    val cb = ApiFile.parseApi(listOf(File(args[0])))
    val builder = FlagApiMap.newBuilder()
    for (pkg in cb.getPackages().packages) {
        val packageName = pkg.qualifiedName()
        pkg.allClasses()
            .filter { it.methods().size > 0 }
            .forEach {
                extractFlaggedApisFromClass(it, it.methods(), packageName, builder)
                extractFlaggedApisFromClass(it, it.constructors(), packageName, builder)
            }
    }
    val flagApiMap = builder.build()
    FileWriter(args[1]).use { it.write(flagApiMap.toString()) }
}

fun extractFlaggedApisFromClass(
    classItem: ClassItem,
    methods: List<MethodItem>,
    packageName: String,
    builder: FlagApiMap.Builder
) {
    val classFlag =
        classItem.modifiers
            .findAnnotation("android.annotation.FlaggedApi")
            ?.findAttribute("value")
            ?.value
            ?.value() as? String
    for (method in methods) {
        val methodFlag =
            method.modifiers
                .findAnnotation("android.annotation.FlaggedApi")
                ?.findAttribute("value")
                ?.value
                ?.value() as? String
                ?: classFlag
        val api =
            JavaMethod.newBuilder()
                .setPackageName(packageName)
                .setClassName(classItem.fullName())
                .setMethodName(method.name())
        for (param in method.parameters()) {
            api.addParameters(param.type().toTypeString())
        }
        if (methodFlag != null) {
            addFlaggedApi(builder, api, methodFlag)
        }
    }
}

fun addFlaggedApi(builder: FlagApiMap.Builder, api: JavaMethod.Builder, flag: String) {
    if (builder.containsFlagToApi(flag)) {
        val updatedApis = builder.getFlagToApiOrThrow(flag).toBuilder().addJavaMethods(api).build()
        builder.putFlagToApi(flag, updatedApis)
    } else {
        val apis = FlaggedApis.newBuilder().addJavaMethods(api).build()
        builder.putFlagToApi(flag, apis)
    }
}
