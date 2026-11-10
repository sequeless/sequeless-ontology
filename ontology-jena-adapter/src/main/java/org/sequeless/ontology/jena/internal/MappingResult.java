package org.sequeless.ontology.jena.internal;

import java.util.List;
import java.util.Map;
import org.apache.jena.ontapi.model.OntModel;
import org.sequeless.ontology.api.OntologyIssue;
import org.sequeless.ontology.automation.api.Action;
import org.sequeless.ontology.automation.api.TransitionRef;
import org.sequeless.ontology.behaviour.api.DerivationRule;
import org.sequeless.ontology.behaviour.api.StateMachineDefinition;
import org.sequeless.ontology.schema.api.TypeDefinition;

/**
 * The outcome of {@link SnapshotMapper#map(OntModel)}: every {@link TypeDefinition} the model
 * contains, deterministically ordered, the state machines and derivation rules that govern them, the
 * actions of each transition, and every {@link OntologyIssue} found while mapping. {@code WARNING}
 * issues (currently only the unmapped-datatype fallback) describe a degraded but still-usable
 * mapping; {@code ERROR} issues describe a malformed {@code sq:derivedBy} rule (a node typed as
 * both/neither {@code sq:Rollup}/{@code sq:Plugin}, an unknown {@code sq:function}/{@code
 * sq:operator}, a missing required term, a {@code sq:lt}/{@code sq:lte}/{@code sq:gt}/{@code sq:gte}
 * comparison on a non-number property, or a {@code sq:via} that is not an object property of the
 * {@code sq:over} type) — {@code JenaOntologyPort.buildStateFrom} folds these into the overall
 * consistency verdict exactly like every other activation-time source of truth, the same way it
 * already does for unresolved imports and reserved terms.
 *
 * @param types every named, non-builtin type in the model, sorted by IRI; carries no behaviour
 * @param stateMachines the state machine of each type that has one, keyed by the type's IRI
 * @param derivations the derivation rule of each derived property, keyed by the property's IRI
 * @param transitionActions the actions of each transition that has any, keyed by {@link
 *     TransitionRef}; where two transitions share a key the first in {@link
 *     StateMachineDefinition#transitions()} order is kept, because the second can never fire
 * @param issues every issue found while mapping, in the order produced; may contain {@code ERROR}
 *     severity for a malformed derivation rule
 */
public record MappingResult(
        List<TypeDefinition> types,
        Map<String, StateMachineDefinition> stateMachines,
        Map<String, DerivationRule> derivations,
        Map<TransitionRef, List<Action>> transitionActions,
        List<OntologyIssue> issues) {}
