/**
 * The Behaviour half of an ontology activation: the state machines that govern object types, the
 * transitions between their states and what makes each transition fire, and the rules that derive
 * property values.
 *
 * <p>Like every package in the ontology contracts, this package imports nothing outside the
 * JDK ({@code java.*}) and the kernel, except sibling types in {@code org.sequeless.ontology.schema.api}
 * and, through {@link org.sequeless.ontology.behaviour.api.RollupRule}, the {@code
 * org.sequeless.filter.api.FilterNode} filter tree that rule carries already resolved. {@link
 * org.sequeless.ontology.behaviour.api.Behaviour} is the aggregate of this half: its state machines are
 * keyed by the IRI of the type each one governs, and its derivation rules by the IRI of the property
 * each one derives. {@link org.sequeless.ontology.behaviour.spi.BehaviourSource} is the read port.
 *
 * <p>A {@link org.sequeless.ontology.behaviour.api.StateMachineDefinition} carries the {@link
 * org.sequeless.ontology.behaviour.api.State}s a type's objects move through, the state they start in,
 * and the {@link org.sequeless.ontology.behaviour.api.Transition}s between them. The actions a transition
 * runs are not here: they are in {@code org.sequeless.ontology.automation.api}. Each {@link
 * org.sequeless.ontology.behaviour.api.DerivationRule} is sealed to {@link
 * org.sequeless.ontology.behaviour.api.RollupRule} (a declarative {@code sq:Rollup} aggregate) and {@link
 * org.sequeless.ontology.behaviour.api.PluginRule} (a named {@code sq:Plugin} dispatched through {@code
 * DerivationPlugin}).
 *
 * <p>Every {@link org.sequeless.ontology.behaviour.api.Transition} carries a {@link
 * org.sequeless.ontology.behaviour.api.TriggerSpec}: sealed to {@link
 * org.sequeless.ontology.behaviour.api.UserActionTrigger} ({@code sq:UserAction}, a user's own REST
 * request), {@link org.sequeless.ontology.behaviour.api.OnChangeTrigger} ({@code sq:OnChange}, a related
 * object's data changing, named via {@code sq:watch}), {@link
 * org.sequeless.ontology.behaviour.api.TimerTrigger} ({@code sq:Timer}, an elapsed {@code sq:after}
 * duration), and {@link org.sequeless.ontology.behaviour.api.ExternalSignalTrigger} ({@code
 * sq:ExternalSignal}, a named {@code sq:signalName} arriving over the signals endpoint).
 * {@link org.sequeless.ontology.behaviour.api.TriggerKind} is the plain enum discriminator shared by all
 * four, used where a full {@code TriggerSpec} pattern-match would be overkill.
 */
package org.sequeless.ontology.behaviour.api;
