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
package org.jacoco.core.internal.analysis.filter;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Filters code that is generated by ECJ for a <code>switch</code> statement
 * with a <code>String</code> and by Kotlin compiler 1.5 and above for a
 * <code>when</code> expression with a <code>String</code>.
 */
final class StringSwitchFilter implements IFilter {

	public void filter(final MethodNode methodNode,
			final IFilterContext context, final IFilterOutput output) {
		final Matcher matcher = new Matcher();
		for (final AbstractInsnNode i : methodNode.instructions) {
			matcher.match(i, output);
		}
	}

	private static class Matcher extends AbstractMatcher {
		public void match(final AbstractInsnNode start,
				final IFilterOutput output) {
			if (start.getOpcode() != Opcodes.ASTORE) {
				return;
			}
			vars.put("s", (VarInsnNode) start);
			cursor = start;
			JumpInsnNode ifNullInstruction = null;
			if (start.getNext().getOpcode() == Opcodes.ALOAD) {
				// Kotlin
				nextIsVar(Opcodes.ALOAD, "s");
				if (cursor == null) {
					return;
				} else if (cursor.getNext().getOpcode() == Opcodes.DUP) {
					// nullable case
					nextIs(Opcodes.DUP);
					nextIs(Opcodes.IFNULL);
					ifNullInstruction = (JumpInsnNode) cursor;
				} else if (cursor.getNext().getOpcode() == Opcodes.IFNULL) {
					// nullable else
					nextIs(Opcodes.IFNULL);
					ifNullInstruction = (JumpInsnNode) cursor;
					nextIsVar(Opcodes.ALOAD, "s");
				}
			}
			nextIsInvoke(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode",
					"()I");
			nextIsSwitch();
			if (cursor == null) {
				return;
			}

			final AbstractInsnNode s = cursor;
			final int hashCodes;
			final LabelNode defaultLabel;
			if (s.getOpcode() == Opcodes.LOOKUPSWITCH) {
				final LookupSwitchInsnNode lookupSwitch = (LookupSwitchInsnNode) cursor;
				defaultLabel = lookupSwitch.dflt;
				hashCodes = lookupSwitch.labels.size();
			} else {
				final TableSwitchInsnNode tableSwitch = (TableSwitchInsnNode) cursor;
				defaultLabel = tableSwitch.dflt;
				hashCodes = tableSwitch.labels.size();
			}

			if (hashCodes == 0) {
				return;
			}

			final Set<AbstractInsnNode> replacements = new HashSet<AbstractInsnNode>();
			replacements.add(skipNonOpcodes(defaultLabel));

			for (int i = 0; i < hashCodes; i++) {
				while (true) {
					nextIsVar(Opcodes.ALOAD, "s");
					nextIs(Opcodes.LDC);
					nextIsInvoke(Opcodes.INVOKEVIRTUAL, "java/lang/String",
							"equals", "(Ljava/lang/Object;)Z");
					// jump to case
					nextIs(Opcodes.IFNE);
					if (cursor == null) {
						return;
					}

					replacements
							.add(skipNonOpcodes(((JumpInsnNode) cursor).label));

					if (cursor.getNext().getOpcode() == Opcodes.GOTO) {
						// end of comparisons for same hashCode
						// jump to default
						nextIs(Opcodes.GOTO);
						break;
					} else if (cursor.getNext() == defaultLabel) {
						break;
					}
				}
			}

			if (ifNullInstruction != null) {
				replacements.add(skipNonOpcodes(ifNullInstruction.label));
			}

			output.ignore(start.getNext(), cursor);
			output.replaceBranches(start, replacements);
		}
	}

}
