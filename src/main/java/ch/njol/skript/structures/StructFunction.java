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
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.function.FunctionEvent;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.lang.function.Signature;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class StructFunction extends Structure {

	public static final Priority PRIORITY = new Priority(30);

	static {
		Skript.registerStructure(StructFunction.class, "function <.+>");
	}

	private SectionNode node;
	@Nullable
	private Signature<?> signature;

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult, SectionNode node) {
		this.node = node;
		return true;
	}

	@Override
	public void preload() {

	}

	@Override
	public void load() {
		getParser().setCurrentEvent("function", FunctionEvent.class);

		if (getParser().getCurrentScript() == null)
			throw new IllegalStateException("Current script is null during function loading");
		signature = Functions.loadSignature(getParser().getCurrentScript().getFileName(), node);

		getParser().deleteCurrentEvent();
	}

	@Override
	public void unload() {
		if (signature != null) {
			Functions.unregisterFunction(signature);
		}
	}

	@Override
	public Priority getPriority() {
		return PRIORITY;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "function";
	}

}
