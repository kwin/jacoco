/*******************************************************************************
 * Copyright (c) 2009, 2024 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Evgeny Mandrikov - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.core.test.validation.kotlin.targets

import org.jacoco.core.test.validation.targets.Stubs.execSerializable
import org.jacoco.core.test.validation.targets.Stubs.nop

/**
 * Test target with [java.io.Serializable] lambda.
 */
object KotlinLambdaSerializableTarget {

    @JvmStatic
    fun main(args: Array<String>) {

        execSerializable {
            nop() // assertFullyCovered()
        }

    }

}
