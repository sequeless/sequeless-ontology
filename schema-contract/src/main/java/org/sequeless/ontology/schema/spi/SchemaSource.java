package org.sequeless.ontology.schema.spi;

import org.sequeless.kernel.identity.TenantId;
import org.sequeless.ontology.schema.api.Schema;

/**
 * The read port for the Schema half of the active ontology. Every implementation must satisfy this
 * read-port contract:
 *
 * <ul>
 *   <li>A {@code null} tenant throws {@link NullPointerException}.
 *   <li>Reads are deterministic and have no side effects between activations.
 *   <li>One call returns the half of exactly one Ontology version.
 *   <li>The port throws {@link NoConsistentOntologyException} when no consistent Ontology is
 *       active. An adapter may throw a subtype of it.
 * </ul>
 */
public interface SchemaSource {

    /**
     * Returns the Schema of the Ontology version active for {@code tenant}. See the interface
     * javadoc for the read-port contract.
     *
     * @param tenant the tenant whose active ontology is read; must not be {@code null}
     * @return the Schema of exactly one Ontology version
     * @throws NullPointerException if {@code tenant} is {@code null}
     * @throws NoConsistentOntologyException if no consistent Ontology is active for {@code tenant}
     */
    Schema schema(TenantId tenant);
}
