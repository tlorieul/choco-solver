/**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package solver.search.loop.lns.neighbors;

import solver.ICause;
import solver.exception.ContradictionException;
import solver.search.restart.GeometricalRestartStrategy;
import solver.search.restart.IRestartStrategy;
import solver.variables.IntVar;

import java.util.BitSet;
import java.util.Random;

/**
 * A Random LNS
 * <p/>
 *
 * @author Charles Prud'homme
 * @since 18/04/13
 */
public class RandomNeighborhood implements INeighbor {

    IRestartStrategy geo;
    private final int n;
    private final double factor;
    private final IntVar[] vars;
    private final int[] bestSolution;
    private Random rd;
    private double nbFixedVariables = 0d;
    private int nbCall, limit;

    BitSet fragment;  // index of variable to set unfrozen

    public RandomNeighborhood(IntVar[] vars, long seed, double factor) {
        this.n = vars.length;
        assert factor > 1.0;
        this.factor = factor;
        this.vars = vars.clone();

        this.rd = new Random(seed);
        this.bestSolution = new int[n];
        this.fragment = new BitSet(n);
        geo = new GeometricalRestartStrategy(n / 2, 1.01);
    }

    @Override
    public boolean isSearchComplete() {
        return nbFixedVariables < 1;
    }

    @Override
    public void recordSolution() {
        for (int i = 0; i < vars.length; i++) {
            bestSolution[i] = vars[i].getValue();
        }
        nbFixedVariables = 2. * n / 3. + 1;
        nbCall = 0;
        limit = geo.getNextCutoff(nbCall);
    }

    @Override
    public void fixSomeVariables(ICause cause) throws ContradictionException {
        nbCall++;
        if (nbCall > limit) {
            limit = nbCall + geo.getNextCutoff(nbCall);
            restrictLess();
        }
        fragment.set(0, n); // all variables are frozen
        for (int i = 0; i < nbFixedVariables - 1 && fragment.cardinality() > 0; i++) {
            int id = selectVariable();
            if (vars[id].contains(bestSolution[id])) {  // to deal with objective variable and related
                vars[id].instantiateTo(bestSolution[id], cause);
            }
            fragment.clear(id);
        }
    }

    private int selectVariable() {
        int id;
        int cc = rd.nextInt(fragment.cardinality());
        for (id = fragment.nextSetBit(0); id >= 0 && cc > 0; id = fragment.nextSetBit(id + 1)) {
            cc--;
        }
        return id;
    }

    @Override
    public void restrictLess() {
        nbFixedVariables /= factor;
    }
}
