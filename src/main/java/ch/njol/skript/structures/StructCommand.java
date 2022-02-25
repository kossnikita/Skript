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

import ch.njol.skript.ScriptLoader.ScriptInfo;
import ch.njol.skript.Skript;
import ch.njol.skript.command.CommandEvent;
import ch.njol.skript.command.Commands;
import ch.njol.skript.command.ScriptCommand;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class StructCommand extends Structure {

	public static final Priority PRIORITY = new Priority(10);

	static {
		Skript.registerStructure(StructCommand.class, "command <.+>");
	}

	private SectionNode sectionNode;
	@Nullable
	private ScriptCommand command;

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult, SectionNode node) {
		this.sectionNode = node;

		return true;
	}

	@Override
	public void preload() {

	}

	@Override
	public void load() {
		ScriptInfo scriptInfo = getParser().getScriptInfo();

		getParser().setCurrentEvent("command", CommandEvent.class);

		// TODO split
		command = Commands.loadCommand(sectionNode, false);

		getParser().deleteCurrentEvent();

		if (command != null) {
			scriptInfo.commandNames.add(command.getName()); // For tab completion
			scriptInfo.commands++;
		}

		Commands.registerCommand(command);
	}

	@Override
	public void unload() {
		if (command != null)
			Commands.unregister(command);
	}

	@Override
	public Priority getPriority() {
		return PRIORITY;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "command ";
	}

}
