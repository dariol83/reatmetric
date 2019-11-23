package eu.dariolucia.reatmetric.processing.impl.graph;

import java.util.Objects;

public class DependencyEdge {

    private final EntityVertex source;
    private final EntityVertex destination;

    public DependencyEdge(EntityVertex source, EntityVertex destination) {
        this.source = source;
        this.destination = destination;
        this.source.getSuccessors().add(this);
        this.destination.getPredecessors().add(this);
    }

    public EntityVertex getSource() {
        return source;
    }

    public EntityVertex getDestination() {
        return destination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyEdge that = (DependencyEdge) o;
        return source.equals(that.source) &&
                destination.equals(that.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, destination);
    }
}
