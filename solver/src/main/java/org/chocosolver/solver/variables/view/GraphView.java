/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2021, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.variables.view;

import org.chocosolver.solver.ICause;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.learn.ExplanationForSignedClause;
import org.chocosolver.solver.variables.GraphVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.delta.GraphDelta;
import org.chocosolver.solver.variables.events.GraphEventType;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.solver.variables.impl.scheduler.GraphEvtScheduler;
import org.chocosolver.util.iterators.EvtScheduler;
import org.chocosolver.util.objects.graphs.IGraph;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;

/**
 * An abstract class for graph views over other variables
 *
 * @author Dimitri Justeau-Allaire
 * @since 31/03/2021
 */
public abstract class GraphView<V extends Variable, E extends IGraph> extends AbstractView<V> implements GraphVar<E> {

    protected GraphDelta delta;
    protected boolean reactOnModification;

    /**
     * Creates a graph view.
     *
     * @param name      name of the view
     * @param variables observed variables
     */
    protected GraphView(String name, V... variables) {
        super(name, variables);
    }

    protected abstract boolean doRemoveNode(int node) throws ContradictionException;

    protected abstract boolean doEnforceNode(int node) throws ContradictionException;

    protected abstract boolean doRemoveEdge(int from, int to) throws ContradictionException;

    protected abstract boolean doEnforceEdge(int from, int to) throws ContradictionException;


    @Override
    public boolean enforceNode(int node, ICause cause) throws ContradictionException {
        assert cause != null;
        assert (node >= 0 && node < getNbMaxNodes());
        if (!getPotentialNodes().contains(node)) {
            this.contradiction(cause, "enforce node which is not in the domain");
            return false;
        }
        if (doEnforceNode(node)) {
            if (reactOnModification) {
                delta.add(node, GraphDelta.NODE_ENFORCED, cause);
            }
            GraphEventType e = GraphEventType.ADD_NODE;
            notifyPropagators(e, cause);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeNode(int node, ICause cause) throws ContradictionException {
        assert cause != null;
        assert (node >= 0 && node < getNbMaxNodes());
        if (getMandatoryNodes().contains(node)) {
            this.contradiction(cause, "remove mandatory node");
            return false;
        } else if (!getPotentialNodes().contains(node)) {
            return false;
        }
        ISet nei = getPotentialSuccessorsOf(node);
        for (int i : nei) {
            removeEdge(node, i, cause);
        }
        nei = getPotentialPredecessorOf(node);
        for (int i : nei) {
            removeEdge(i, node, cause);
        }
        if (doRemoveNode(node)) {
            if (reactOnModification) {
                delta.add(node, GraphDelta.NODE_REMOVED, cause);
            }
            GraphEventType e = GraphEventType.REMOVE_NODE;
            notifyPropagators(e, cause);
            return true;
        }
        return false;
    }

    @Override
    public boolean enforceEdge(int x, int y, ICause cause) throws ContradictionException {
        assert cause != null;
        enforceNode(x, cause);
        enforceNode(y, cause);
        if (!getPotentialSuccessorsOf(x).contains(y)) {
            this.contradiction(cause, "enforce edge which is not in the domain");
            return false;
        }
        if (doEnforceEdge(x, y)) {
            if (reactOnModification) {
                delta.add(x, GraphDelta.EDGE_ENFORCED_TAIL, cause);
                delta.add(y, GraphDelta.EDGE_ENFORCED_HEAD, cause);
            }
            GraphEventType e = GraphEventType.ADD_EDGE;
            notifyPropagators(e, cause);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeEdge(int x, int y, ICause cause) throws ContradictionException {
        assert cause != null;
        if (getMandatorySuccessorsOf(x).contains(y)) {
            this.contradiction(cause, "remove mandatory edge");
            return false;
        }
        if (doRemoveEdge(x, y)) {
            if (reactOnModification) {
                delta.add(x, GraphDelta.EDGE_REMOVED_TAIL, cause);
                delta.add(y, GraphDelta.EDGE_REMOVED_HEAD, cause);
            }
            GraphEventType e = GraphEventType.REMOVE_EDGE;
            notifyPropagators(e, cause);
            return true;
        }
        return false;
    }

    @Override
    public void instantiateTo(IGraph value, ICause cause) throws ContradictionException {
        for (int i = 0; i < getNbMaxNodes(); i++) {
            if (value.getNodes().contains(i)) {
                enforceNode(i, cause);
            } else if (getUB().containsNode(i)) {
                removeNode(i, cause);
            }
        }
        for (int i = 0; i < getNbMaxNodes(); i++) {
            for (int j = 0; j < getNbMaxNodes(); j++) {
                if (value.getNodes().contains(i) && value.getNodes().contains(j)) {
                    if (value.getSuccessorsOf(i).contains(j)) {
                        enforceEdge(i, j, cause);
                    } else if (getUB().containsEdge(i, j)) {
                        removeEdge(i, j, cause);
                    }
                }
            }
        }
    }

    @Override
    public GraphDelta getDelta() {
        return delta;
    }

    @Override
    public void createDelta() {
        if (!reactOnModification) {
            reactOnModification = true;
            delta = new GraphDelta(getEnvironment());
        }
    }

    @Override
    public int getTypeAndKind() {
        return VIEW | GRAPH;
    }

    @Override
    protected EvtScheduler createScheduler() {
        return new GraphEvtScheduler();
    }

    @Override
    public void explain(int pivot, ExplanationForSignedClause explanation) {
        throw new UnsupportedOperationException("GraphView does not support explanation.");
    }

    @Override
    public void justifyEvent(IntEventType mask, int one, int two, int three) {
        throw new UnsupportedOperationException("GraphView does not support explanation.");
    }
}
