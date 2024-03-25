/*
 * Copyright (C) 2020 Jayakumar Muthukumarasamy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datanapis.xbrl.analysis;

import io.datanapis.xbrl.XbrlInstance;
import io.datanapis.xbrl.model.Concept;
import io.datanapis.xbrl.model.Context;
import io.datanapis.xbrl.model.Fact;
import io.datanapis.xbrl.model.RoleType;
import io.datanapis.xbrl.model.arc.CalculationArc;
import io.datanapis.xbrl.model.link.CalculationLink;
import io.datanapis.xbrl.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class CalculationNetwork extends CalculationTaxonomy {
    private static final Logger log = LoggerFactory.getLogger(CalculationNetwork.class);

    public enum Result {
        COMPLETE("Complete"),
        PARTIAL("Partial"),
        ROOT_LEVEL_COMPLETE("Complete at root level");

        private final String result;

        Result(String result) {
            this.result = result;
        }

        public String toString() {
            return this.result;
        }

        /* Here is the ordering relationship: PARTIAL < ROOT_LEVEL_COMPLETE < COMPLETE */
        static int compare(Result lhs, Result rhs) {
            switch (lhs) {
                case COMPLETE:
                    return switch (rhs) {
                        case PARTIAL, ROOT_LEVEL_COMPLETE -> 1;
                        default -> 0;
                    };
                case PARTIAL:
                    return switch (rhs) {
                        case COMPLETE, ROOT_LEVEL_COMPLETE -> -1;
                        default -> 0;
                    };
                default:
                    /* i.e. ROOT_LEVEL_COMPLETE: */
                    return switch (rhs) {
                        case PARTIAL -> 1;
                        case COMPLETE -> -1;
                        default -> 0;
                    };
            }
        }
    }

    private final XbrlInstance instance;
    private final boolean debug;
    private final CalculationProcessor processor;

    public CalculationNetwork(XbrlInstance instance, CalculationProcessor processor) {
        this(instance, processor,false);
    }

    public CalculationNetwork(XbrlInstance instance, CalculationProcessor processor, boolean debug) {
        super(instance.getTaxonomy());
        this.instance = instance;
        this.debug = debug;
        this.processor = processor;
    }

    public void validateCalculation(RoleType roleType) {
        this.validateCalculation(null, roleType);
    }

    public void validateCalculation(PrintWriter writer, RoleType roleType) {
        CalculationLink calculationLink = roleType.getCalculationLink();
        if (calculationLink == null)
            return;

        Collection<CalculationGraphNode> graphNodes =
                CalculationTaxonomy.getRootNodes(instance.getTaxonomy(), calculationLink);
        if (graphNodes.isEmpty())
            return;

        Collection<Context>contexts = instance.getAllContexts();

        if (writer != null)
            writer.println("Calculation:");

        processor.calculationStart(roleType);
        for (CalculationGraphNode node : graphNodes) {
            processor.calculationRootStart(node);
            try {
                Result result = calculate(node, writer, contexts);
                if (result == Result.PARTIAL) {
                    if (writer != null) {
                        writer.println("************** Calculation check returned partial **************");
                        node.displayNetwork(writer);
                        writer.println();
                    }
                }
            } catch (Exception e) {
                if (writer != null) {
                    node.displayNetwork(writer);
                    writer.println();
                }
            }
            processor.calculationRootEnd(node);
        }
        processor.calculationEnd(roleType);
    }

    private Result calculate(CalculationGraphNode root, Collection<Context> contexts) {
        return this.calculate(root,null, contexts);
    }

    private Result calculate(CalculationGraphNode root, PrintWriter writer, Collection<Context> contexts) {
        assert (root.getArc() == null);

        // TODO: Do we need to assert for monetary facts?

        List<Context> rootContexts = new ArrayList<>();
        for (Context context : contexts) {
            for (Fact fact : context.getFacts()) {
                if (fact.getConcept().equals(root.getConcept())) {
                    rootContexts.add(context);
                    break;
                }
            }
        }

        rootContexts.sort(Comparator.comparing(Context::getPeriod));
        String prefix = " ".repeat(4);

        // rootContexts.size() may be 0. This can happen if the Calculation network contains
        // a concept but the XBRL instance does not include it.
        Result result = Result.COMPLETE;
        for (Context primary : rootContexts) {
            processor.calculationPeriodStart(root, primary);
            if (writer != null && primary.hasDimensions()) {
                writer.printf("%s %s: %s\n", prefix, primary.getPeriod(), Utils.join(primary.getDimensions()));
            }
            Result contextResult = calculate(root, 1, writer, primary);
            if (Result.compare(contextResult, result) < 0) {
                result = contextResult;
            }
            processor.calculationPeriodEnd(root, primary);
        }

        return result;
    }

    private static double getValue(Fact fact) {
        if (fact.getLongValue() != null) {
            return (double)fact.getLongValue();
        } else if (fact.getDoubleValue() != null) {
            return fact.getDoubleValue();
        } else {
            throw new RuntimeException("Fact has non-computable value!!");
        }
    }

    private static long round(double value, int decimals) {
        double multiplier = Math.pow(10, decimals);
        long rounded = Math.round(value * multiplier);
        return rounded;
    }

    private Result calculate(CalculationGraphNode parent, int level, PrintWriter writer, Context context) {
        Concept parentConcept = parent.getConcept();
        Fact summation = context.getFact(parent.getConcept());
        if (summation == null)
            return Result.COMPLETE;

        String prefix = " ".repeat(level * 4);
        double value = getValue(summation);
        int decimals = summation.getDecimals();

        Concept.Balance balance = parentConcept.getBalance();
        processor.calculationNodeStart(level, parent, summation);
        if (writer != null) {
            writer.printf("%s validating [%s] = [%.2f] [%s]:\n",
                    prefix, parentConcept.getQualifiedName(), value, balance.toString());
        }
        log.debug("{} validating [{}] = [{}] [{}]:\n",
                prefix, parentConcept.getQualifiedName(), value, balance.toString());
        if (parent.getOutLinks().isEmpty()) {
            processor.calculationNodeEnd(level, parent, Result.COMPLETE, value);
            return Result.COMPLETE;
        }

        int outLinksVisited = 0;
        Result result = Result.COMPLETE;
        double computed = 0.0;
        for (GraphNode<CalculationArc> child : parent.getOutLinks()) {
            CalculationGraphNode graphNode = (CalculationGraphNode)child;
            Result subresult = calculate(graphNode, level + 1, writer, context);
            if (Result.compare(subresult, result) < 0) {
                /* We will propagate the worst result up the tree */
                result = subresult;
            }

            CalculationArc arc = child.getArc();
            Concept childConcept = child.getConcept();
            Fact item = context.getFact(childConcept);
            if (item == null)
                continue;

            ++outLinksVisited;
            double itemValue = getValue(item);
            switch (childConcept.getBalance()) {
                case DEBIT:
                    if (balance == Concept.Balance.DEBIT) {
                        assert arc.getWeight() > 0;
                    } else if (balance == Concept.Balance.CREDIT) {
                        assert arc.getWeight() < 0;
                    }
                    if (debug && writer != null) {
                        writer.printf(" %s [%.2f] + [%4.2f] * [%.2f] = ", prefix, computed, arc.getWeight(), itemValue);
                    }
                    log.debug("  {} [{}] + [{}] * [{}] = ", prefix, computed, arc.getWeight(), itemValue);
                    computed += arc.getWeight() * itemValue;
                    if (debug && writer != null) {
                        writer.printf("[%.2f]\n", computed);
                    }
                    log.debug("[{}]\n", computed);
                    break;
                case CREDIT:
                    if (balance == Concept.Balance.CREDIT) {
                        assert arc.getWeight() > 0;
                    } else if (balance == Concept.Balance.DEBIT) {
                        assert arc.getWeight() < 0;
                    }
                    if (debug && writer != null) {
                        writer.printf(" %s [%.2f] + [%4.2f] * [%.2f] = ", prefix, computed, arc.getWeight(), itemValue);
                    }
                    log.debug("  {} [{}] + [{}] * [{}] = ", prefix, computed, arc.getWeight(), itemValue);
                    computed += arc.getWeight() * itemValue;
                    if (debug && writer != null) {
                        writer.printf("[%.2f]\n", computed);
                    }
                    log.debug("[{}]\n", computed);
                    break;
                case NONE:
                    if (debug && writer != null) {
                        writer.printf(" %s [%.2f] + [%4.2f] * [%.2f] = ", prefix, computed, arc.getWeight(), itemValue);
                    }
                    log.debug("  {} [{}] + [{}] * [{}] = ", prefix, computed, arc.getWeight(), itemValue);
                    computed += arc.getWeight() * itemValue;
                    if (debug && writer != null) {
                        writer.printf("[%.2f]\n", computed);
                    }
                    log.debug("[{}]\n", computed);
                    break;
            }
        }

        int k = Long.compare(round(value, decimals), round(computed, decimals));
        if (outLinksVisited > 0 && k != 0) {
            /* Value computed using the child nodes does not match the value at the root level */
            if (debug && writer != null) {
                writer.printf("%s ******** [%s] -> [%.2f] = [%.2f]\n",
                        prefix, parentConcept.getQualifiedName(), value, computed);
            }
            log.debug("{} ******** [{}] -> [{}] != [{}]\n", prefix, parentConcept.getQualifiedName(), value, computed);
            processor.calculationNodeEnd(level, parent, Result.PARTIAL, computed);
            return Result.PARTIAL;
        }

        // If we have reached here, then the value calculated using the immediate child nodes matches
        // the value at the root node. However, it is still possible that the result in an inner node
        // did not fully match. If all calculations match, we will return Result.COMPLETE which is the
        // best case. However, if the inner calculations did not match, we will return ROOT_LEVEL_COMPLETE,
        // meaning the calculation matches at the root level but did not match at some inner level.
        // This isn't bad. XBRL calculation networks are not always complete and this is ok by design!
        // In fact, XBRL calculations has several short-comings. See https://www.compsciresources.com/calculated-risk
        result = (result == Result.COMPLETE) ? Result.COMPLETE : Result.ROOT_LEVEL_COMPLETE;
        processor.calculationNodeEnd(level, parent, result, computed);
        return result;
    }
}
