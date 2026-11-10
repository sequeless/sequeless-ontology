package org.sequeless.ontology.automation.spi;

import org.sequeless.kernel.identity.TenantId;
import org.sequeless.ontology.automation.api.Automation;

/**
 * The read port for the Automation half of the active ontology. Every implementation must satisfy
 * this read-port contract:
 *
 * <ul>
 *   <li>A {@code null} tenant throws {@link NullPointerException}.
 *   <li>Reads are deterministic and have no side effects between activations.
 *   <li>One call returns the half of exactly one Ontology version.
 *   <li>The port throws an unchecked exception when no consistent Ontology is active. The adapter
 *       throws {@code org.sequeless.ontology.api.OntologyException}.
 * </ul>
 */
public interface AutomationSource {

    /**
     * Returns the Automation of the Ontology version active for {@code tenant}. See the interface
     * javadoc for the read-port contract.
     *
     * @param tenant the tenant whose active ontology is read; must not be {@code null}
     * @return the Automation of exactly one Ontology version
     * @throws NullPointerException if {@code tenant} is {@code null}
     */
    Automation automation(TenantId tenant);
}
