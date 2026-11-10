/**
 * The Automation half of an ontology activation: the actions that run when a transition fires.
 *
 * <p>Like every package in the ontology contracts, this package imports nothing outside the
 * JDK ({@code java.*}) and the kernel, except sibling types in {@code org.sequeless.ontology.schema.api}
 * and {@code org.sequeless.ontology.behaviour.api}. Every action is sealed to {@link
 * org.sequeless.ontology.automation.api.SetPropertyAction} ({@code sq:SetProperty}), {@link
 * org.sequeless.ontology.automation.api.CreateObjectAction} ({@code sq:CreateObject}), {@link
 * org.sequeless.ontology.automation.api.WebhookAction} ({@code sq:Webhook}), or {@link
 * org.sequeless.ontology.automation.api.LogAction} ({@code sq:Log}) through the {@link
 * org.sequeless.ontology.automation.api.Action} sealed interface, so a {@code switch} over an action is
 * exhaustive without a default case.
 *
 * <p>{@link org.sequeless.ontology.automation.api.Automation} is the aggregate of this half. It files the
 * actions of each transition under a {@link org.sequeless.ontology.automation.api.TransitionRef}; a
 * transition without actions has no entry. {@link org.sequeless.ontology.automation.spi.AutomationSource}
 * is the read port.
 *
 * <p>The triggers that make a transition fire stay on the behaviour half, on {@code
 * org.sequeless.ontology.behaviour.api.Transition}.
 */
package org.sequeless.ontology.automation.api;
