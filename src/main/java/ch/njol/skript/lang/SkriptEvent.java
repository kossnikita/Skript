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
package ch.njol.skript.lang;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.SkriptEventHandler;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.events.EvtClick;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.structures.Structure;
import ch.njol.skript.util.Task;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * A SkriptEvent is like a condition. It is called when any of the registered events occurs.
 * An instance of this class should then check whether the event applies
 * (e.g. the rightclick event is included in the PlayerInteractEvent which also includes lefclicks, thus the SkriptEvent {@link EvtClick} checks whether it was a rightclick or
 * not).<br/>
 * It is also needed if the event has parameters.
 *
 * @see Skript#registerEvent(String, Class, Class, String...)
 * @see Skript#registerEvent(String, Class, Class[], String...)
 */
public abstract class SkriptEvent extends Structure implements SyntaxElement, Debuggable {

	@Nullable
	EventPriority eventPriority;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult, SectionNode node) {
		SyntaxElementInfo<? extends Structure> syntaxElementInfo = getSyntaxElementInfo();
		if (!(syntaxElementInfo instanceof SkriptEventInfo))
			throw new IllegalStateException();
		SkriptEventInfo<?> skriptEventInfo = (SkriptEventInfo<?>) syntaxElementInfo;

		String expr = parseResult.expr;
		if (StringUtils.startsWithIgnoreCase(expr, "on "))
			expr = expr.substring("on ".length());

		String[] split = expr.split(" with priority ");
		if (split.length != 1) {
			if (this instanceof SelfRegisteringSkriptEvent) {
				Skript.error("This event doesn't support event priority");
				return false;
			}

			expr = String.join(" with priority ", Arrays.copyOfRange(split, 0, split.length - 1));

			String priorityString = split[split.length - 1];
			try {
				eventPriority = EventPriority.valueOf(priorityString.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException(e);
			}
		} else {
			eventPriority = null;
		}

		Literal<?>[] literals = Arrays.copyOf(exprs, exprs.length, Literal[].class);
		ParseResult newParseResult = new ParseResult(expr, literals);
		newParseResult.regexes.addAll(parseResult.regexes);
		newParseResult.mark = parseResult.mark;

		// Initiate
		if (!init(literals, matchedPattern, newParseResult))
			return false;

		if (!shouldLoadEvent())
			return true;

		if (Skript.debug() || node.debug())
			Skript.debug(expr + " (" + this + "):");

		List<ParsedEventData> events = getParser().getData(EventData.class).events;

		try {
			getParser().setCurrentEvent(skriptEventInfo.getName().toLowerCase(Locale.ENGLISH), skriptEventInfo.events);
			getParser().setCurrentSkriptEvent(this);

			events.add(new ParsedEventData(skriptEventInfo, this, expr, node, ScriptLoader.loadItems(node)));
		} finally {
			getParser().deleteCurrentEvent();
			getParser().deleteCurrentSkriptEvent();
		}

		if (this instanceof SelfRegisteringSkriptEvent)
			((SelfRegisteringSkriptEvent) this).afterParse(Objects.requireNonNull(getParser().getCurrentScript()));

		return true;
	}

	/**
	 * called just after the constructor
	 */
	public abstract boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult);

	/**
	 * Checks whether the given Event applies, e.g. the leftclick event is only part of the PlayerInteractEvent, and this checks whether the player leftclicked or not. This method
	 * will only be called for events this SkriptEvent is registered for.
	 * @return true if this is SkriptEvent is represented by the Bukkit Event or false if not
	 */
	public abstract boolean check(Event e);

	/**
	 * Script loader checks this before loading items in event. If false is
	 * returned, they are not parsed and the event is not registered.
	 * @return If this event should be loaded.
	 */
	public boolean shouldLoadEvent() {
		return true;
	}

	/**
	 * @return the Event classes to use in {@link ch.njol.skript.lang.parser.ParserInstance},
	 * or {@code null} if the Event classes this SkriptEvent was registered with should be used.
	 */
	public Class<? extends Event> @Nullable[] getEventClasses() {
		return null;
	}

	/**
	 * @return the {@link EventPriority} to be used for this event.
	 * Defined by the user-specified priority, or otherwise the default event priority.
	 */
	public EventPriority getEventPriority() {
		return eventPriority != null ? eventPriority : SkriptConfig.defaultEventPriority.value();
	}

	static {
		ParserInstance.registerData(EventData.class, EventData::new);
	}

	public static class EventData extends ParserInstance.Data {
		public final List<ParsedEventData> events = new ArrayList<>();

		public EventData(ParserInstance parserInstance) {
			super(parserInstance);
		}

		@Override
		public void onCurrentScriptChange(@Nullable Config oldConfig, @Nullable Config newConfig) {
			if (events.isEmpty())
				return;

			if (oldConfig == null)
				throw new IllegalStateException();

			Callable<Void> callable = () -> {
				for (ParsedEventData event : events) {
					getParser().setCurrentEvent(event.info.getName().toLowerCase(Locale.ENGLISH), event.info.events);
					getParser().setCurrentSkriptEvent(event.skriptEvent);

					Trigger trigger;
					try {
						trigger = new Trigger(oldConfig.getFile(), event.event, event.skriptEvent, event.items);
						trigger.setLineNumber(event.node.getLine()); // Set line number for debugging
						trigger.setDebugLabel(oldConfig.getFileName() + ": line " + event.node.getLine());
					} finally {
						getParser().deleteCurrentEvent();
					}

					if (event.skriptEvent instanceof SelfRegisteringSkriptEvent) {
						((SelfRegisteringSkriptEvent) event.skriptEvent).register(trigger);
						SkriptEventHandler.addSelfRegisteringTrigger(trigger);
					} else {
						SkriptEventHandler.addTrigger(event.info.events, trigger);
					}

					getParser().deleteCurrentEvent();
					getParser().deleteCurrentSkriptEvent();
				}
				events.clear();
				return null;
			};

			if (ScriptLoader.isAsync()) { // Need to delegate to main thread
				Task.callSync(callable);
			} else { // We are in main thread, execute immediately
				try {
					callable.call();
				} catch (Exception e) {
					//noinspection ThrowableNotThrown
					Skript.exception(e);
				}
			}
		}
	}

	private static class ParsedEventData {
		public final SkriptEventInfo<?> info;
		public final SkriptEvent skriptEvent;
		public final String event;
		public final SectionNode node;
		public final List<TriggerItem> items;

		public ParsedEventData(SkriptEventInfo<?> info, SkriptEvent skriptEvent, String event, SectionNode node, List<TriggerItem> items) {
			this.info = info;
			this.skriptEvent = skriptEvent;
			this.event = event;
			this.node = node;
			this.items = items;
		}
	}

	/**
	 * @return whether this SkriptEvent supports event priorities
	 */
	public boolean isEventPrioritySupported() {
		return true;
	}

}
