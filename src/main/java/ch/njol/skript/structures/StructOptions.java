/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript.structures;

import ch.njol.skript.Skript;
import ch.njol.skript.config.EntryNode;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class StructOptions extends PreloadingStructure {

	static {
		Skript.registerPreloadingStructure(StructOptions.class, 10, "options");
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult, SectionNode node) {
		node.convertToEntries(-1);
		registerOptions(node, "", true);
		return true;
	}

	@Override
	public void init(SectionNode node) {
		registerOptions(node, "", false);
	}

	private void registerOptions(SectionNode sectionNode, String prefix, boolean error) {
		for (Node n : sectionNode) {
			if (n instanceof EntryNode) {
				getParser().getCurrentOptions().put(prefix + n.getKey(), ((EntryNode) n).getValue());
			} else if (n instanceof SectionNode) {
				registerOptions((SectionNode) n, prefix + n.getKey() + ".", error);
			} else if (error) {
				Skript.error("Invalid line in options");
			}
		}
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "options";
	}

}
