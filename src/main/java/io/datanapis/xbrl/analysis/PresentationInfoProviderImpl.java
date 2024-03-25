package io.datanapis.xbrl.analysis;

import io.datanapis.xbrl.model.*;
import io.datanapis.xbrl.model.arc.PresentationArc;
import lombok.extern.slf4j.Slf4j;

import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
class PresentationInfoProviderImpl implements PresentationInfoProvider {
    private final List<PresentationHypercube> hypercubes;

    PresentationInfoProviderImpl(Collection<PresentationHypercube> hypercubes) {
        /*
         * We used to have one hypercube per info provider, but now we can have multiple. We need the
         * PresentationGraphNode passed in to all the methods to provide the context to identify
         * the correct hypercube. One hypercube per provider is still the common case.
         */
        this.hypercubes = new ArrayList<>(hypercubes);
    }

    PresentationHypercube selectHypercube(PresentationGraphNode node) {
        /* If we have just one hypercube, select it trivially */
        if (hypercubes.size() == 1)
            return hypercubes.get(0);

        /* Multiple hypercubes - walk up from node to the root and find the hypercube that contains this specific node */
        while (node != null) {
            for (PresentationHypercube hypercube : hypercubes) {
                PresentationGraphNode root = (PresentationGraphNode) hypercube.getTable();
                if (root.equals(node))
                    return hypercube;
            }
            node = (PresentationGraphNode) node.getParent();
        }

        throw new RuntimeException("Hypercube not found!");
    }

    private static void logMissingLabel(PresentationArc arc, Concept concept) {
        /* The taxonomy has a preferred arc. However, the concept does not have a corresponding label */
        /* Only log if the label is an uncommon one */
        String preferredLabel = arc.getPreferredLabel();
        if (!preferredLabel.equals(Label.ROLE_TYPE_TERSE_LABEL) && !preferredLabel.equals(Label.ROLE_TYPE_VERBOSE_LABEL)) {
            log.info("Label [{}] missing for Concept [{}]", arc.getPreferredLabel(), concept.getQualifiedName());
        }
    }

    @Override
    public int compare(PresentationGraphNode lhs, PresentationGraphNode rhs) {
        return lhs.compareTo(rhs);
    }

    /**
     * Compare lhsAxis and rhsAxis against the presentation network and return -1, 0 or 1 depending on whether
     * lhsAxis occurs before, at the same position or after rhsAxis in the presentation network. Note, the value
     * can only be 0 if lhsAxis is the same concept as rhsAxis
     *
     * @param lhsAxis the left axis
     * @param rhsAxis the right axis
     * @return -1, 0 and 1 as lhsAxis appears before, at the same position or after rhsAxis in the presentation network
     */
    @Override
    public int compare(PresentationGraphNode node, Concept lhsAxis, Concept rhsAxis) {
        int lhsIndex = -1, rhsIndex = -1;

        PresentationHypercube hypercube = selectHypercube(node);
        List<PresentationHypercube.PresentationAxis> axes = hypercube.getAxes();
        for (int i = 0; i < axes.size(); ++i) {
            PresentationHypercube.PresentationAxis axis = axes.get(i);
            /* In odd cases, an axis is repeated multiple times in the presentation network, we prefer the first one unless it is empty */
            if (axis.getDimension().getConcept().equals(lhsAxis)) {
                if (lhsIndex == -1 || axes.get(lhsIndex).getMembers().isEmpty())
                    lhsIndex = i;
            }
            if (axis.getDimension().getConcept().equals(rhsAxis)) {
                if (rhsIndex == -1 || axes.get(rhsIndex).getMembers().isEmpty())
                    rhsIndex = i;
            }
        }

        if (lhsIndex < rhsIndex)
            return -1;
        else if (lhsIndex > rhsIndex)
            return 1;

        if (!lhsAxis.equals(rhsAxis)) {
            log.info("Compare Concept (lhsAxis, rhsAxis) = 0, when axis aren't the same [{}] vs [{}]", lhsAxis.getQualifiedName(), rhsAxis.getQualifiedName());
        }
        return 0;
    }

    /**
     * Return the presentation axis corresponding to Concept. Note, this method does not expect to fail. So, it
     * is the caller's responsibility to make sure the Concept is a valid one - meaning, it has a corresponding
     * axis mapped to it in this presentation network.
     *
     * @param axisConcept the Concept for which the axis should be returned
     * @return the axis.
     */
    private PresentationHypercube.PresentationAxis getPresentationAxis(PresentationGraphNode node, Concept axisConcept) {
        int index = -1;
        PresentationHypercube hypercube = selectHypercube(node);
        List<PresentationHypercube.PresentationAxis> axes = hypercube.getAxes();
        for (int i = 0; i < axes.size(); ++i) {
            PresentationHypercube.PresentationAxis axis = axes.get(i);
            /* In odd cases, an axis is repeated multiple times in the presentation network, we prefer the first one unless it is empty */
            if (axis.getDimension().getConcept().equals(axisConcept)) {
                if (index == -1 || axes.get(index).getMembers().isEmpty())
                    index = i;
            }
        }

        if (index == -1)
            return null;

        PresentationHypercube.PresentationAxis axis = axes.get(index);
        return axis;
    }

    @Override
    public int compare(PresentationGraphNode node, ExplicitMember lhs, ExplicitMember rhs) {
        Concept lhsAxis = lhs.getDimension();
        Concept rhsAxis = rhs.getDimension();
        int result = compare(node, lhsAxis, rhsAxis);
        if (result != 0)
            return result;

        Concept lhsMember = lhs.getMember();
        Concept rhsMember = rhs.getMember();
        if (lhsMember.equals(rhsMember))
            return 0;

        PresentationHypercube.PresentationAxis axis = getPresentationAxis(node, lhsAxis);
        assert Objects.nonNull(axis);
        for (PresentationGraphNode member : axis.getMembers()) {
            if (member.getConcept().equals(lhsMember)) {
                return -1;
            } else if (member.getConcept().equals(rhsMember)) {
                return 1;
            }
        }

        /* technically, we shouldn't be reaching here. However, member names are not always consistent */
        log.debug("Compare ExplicitMember ([{}, {}], [{}, {}]) = 0. Not expecting to reach here!",
                lhs.getDimension().getQualifiedName(), lhs.getMember().getQualifiedName(),
                rhs.getDimension().getQualifiedName(), rhs.getMember().getQualifiedName());

        /* Since, we have no other way to break ties - we are just going to do a simple string compare */
        return lhs.getMember().getQualifiedName().compareTo(rhs.getMember().getQualifiedName());
    }

    /**
     * compare two DimensionedFacts and return -1, 0 or 1. This is not a straight forward comparison since we need
     * to consider three things - the concept the fact belongs to, the explicit members that qualify a fact or the
     * typed members that qualify a fact. Sequencing simple facts, i.e. without any qualifiers is straight forward.
     * Similarly, sequencing two facts that are both qualified using explicit members is also straight forward.
     * Sequencing two facts where one is qualified using explicit members and the other is qualified using typed
     * members is the tricky situation, especially since compare() needs to be transitively consistent. When
     * comparing facts, we order one with fewer qualifiers lesser than one with more qualifiers. When comparing
     * explicit members with typed members, we order based on axis since there is no way to order based on members.
     *
     * @param lhs the left dimensioned fact
     * @param rhs the right dimensioned fact
     * @return -1, 0 or 1 as the left fact is less than, equal to or greater than the right fact
     */
    private int compare(PresentationGraphNode node, DimensionedFact lhs, DimensionedFact rhs) {
        if (!lhs.isQualified() && !rhs.isQualified()) {
            /* Neither is qualified and both belong to the same concept, just compare the facts */
            return Fact.compare(lhs.getFact(), rhs.getFact());
        }
        /* At least one or both are qualified */
        if (!lhs.isQualified()) {
            return -1;
        } else if (!rhs.isQualified()) {
            return 1;
        }

        /* Both are qualified */
        assert lhs.isQualified() && rhs.isQualified();

        Concept concept = lhs.getFact().getConcept();

        /* If both facts have explicit dimensions, compare them first */
        List<ExplicitMember> left = lhs.getDimensions();
        List<ExplicitMember> right = rhs.getDimensions();
        if (left != null && right != null) {
            assert !left.isEmpty();
            assert !right.isEmpty();

            int i = 0, j = 0;
            while (i < left.size() && j < right.size()) {
                int result = compare(node, left.get(i++), right.get(j++));
                if (result != 0)
                    return result;
            }

            if (i == left.size() && j == right.size()) {
                return Fact.compare(lhs.getFact(), rhs.getFact());
            } else if (i == left.size()) {
                /* left has fewer qualifiers, rank it lower */
                return -1;
            } else {
                /* left has more qualifiers, rank it higher */
                return 1;
            }
        }

        /* either fact may have a typed dimension */
        Iterator<TypedMember> li = lhs.getFact().getContext().getTypedMembers().iterator();
        Iterator<TypedMember> ri = rhs.getFact().getContext().getTypedMembers().iterator();

        /* handle the case where one could have an explicit dimension while the other could have a typed dimension */
        /* order of the logic ensures that if a context has both explicit and typed dimensions, the explicit one will be considered first */
        /* not sure if the semantics of a context having both dimensions is well-defined, but we are not handling it */
        if (left != null && ri.hasNext()) {
            Iterator<ExplicitMember> explicitMemberIterator = left.iterator();
            while (explicitMemberIterator.hasNext() && ri.hasNext()) {
                ExplicitMember explicitMember = explicitMemberIterator.next();
                TypedMember typedMember = ri.next();
                int result = compare(node, explicitMember.getDimension(), typedMember.getDimension());
                if (result != 0)
                    return result;
            }
            ri = rhs.getFact().getContext().getTypedMembers().iterator();
        } else if (li.hasNext() && right != null) {
            Iterator<ExplicitMember> explicitMemberIterator = right.iterator();
            while (li.hasNext() && explicitMemberIterator.hasNext()) {
                TypedMember typedMember = li.next();
                ExplicitMember explicitMember = explicitMemberIterator.next();
                int result = compare(node, typedMember.getDimension(), explicitMember.getDimension());
                if (result != 0)
                    return result;
            }
            li = lhs.getFact().getContext().getTypedMembers().iterator();
        }

        /* this is the case where we only have typed dimensions */
        while (li.hasNext() && ri.hasNext()) {
            TypedMember l = li.next();
            TypedMember r = ri.next();
            int result = l.compareTo(r);
            if (result != 0)
                return result;
        }

        if (li.hasNext()) {
            /* Left has more qualifiers, rank it higher */
            return 1;
        } else if (ri.hasNext()) {
            /* Left has fewer qualifiers, rank it lower */
            return -1;
        }

        return Fact.compare(lhs.getFact(), rhs.getFact());
    }

    /**
     * Run random experiments on a sorted list of DimensionedFacts to see if transitivity of
     * compare(DimensionedFact, DimensionedFact) is violated. This is only used for live debugging.
     *
     * @param facts the facts to test
     */
    private void randomExperiments(PresentationGraphNode node, List<DimensionedFact> facts) {
        Random random = new Random(java.time.Instant.now().getLong(ChronoField.INSTANT_SECONDS));
        for (int trials = 0; trials < 5000; trials++) {
            int i = random.nextInt(facts.size());
            int j = random.nextInt(facts.size());
            int k = random.nextInt(facts.size());

            int ijResult = this.compare(node, facts.get(i), facts.get(j));
            int jkResult = this.compare(node, facts.get(j), facts.get(k));
            int ikResult = this.compare(node, facts.get(i), facts.get(k));

            /* i < j */
            if (ijResult < 0) {
                if (jkResult < 0) {
                    /* Since, i < j < k, i < k should hold */
                    if (!(ikResult < 0)) {
                        int y = 5;
                    }
                } else if (jkResult == 0) {
                    /* Since, i < j == k, i < k should hold */
                    if (!(ikResult < 0)) {
                        int y = 5;
                    }
                }
                /* Since, i < j > k, meaning j is greater than both i and k, we can't infer anything about i and k */
            }
            /* i == j */
            if (ijResult == 0) {
                if (jkResult < 0) {
                    /* Since, i = j < k, i < k should hold */
                    if (!(ikResult < 0)) {
                        int y = 5;
                    }
                } else if (jkResult == 0) {
                    /* Since, i = j = k, i = k should hold */
                    if (!(ikResult == 0)) {
                        int y = 5;
                    }
                } else {
                    /* Since, i = j > k, i > k should hold */
                    if (!(ikResult > 0)) {
                        int y = 5;
                    }
                }
            }
            /* i > j */
            if (ijResult > 0) {
                if (jkResult == 0) {
                    /* Since, i > j = k, i > k should hold */
                    if (!(ikResult > 0)) {
                        int y = 5;
                    }
                } else if (jkResult > 0) {
                    /* Since, i > j > k, i > k should hold */
                    if (!(ikResult > 0)) {
                        int y = 5;
                    }
                }
                /* Since, i > j < k, meaning j is less than both i and k, we can't infer anything about i and k */
            }
        }
    }

    @Override
    public void order(PresentationGraphNode node, List<DimensionedFact> facts) {
        /* All facts are guaranteed to belong to the same concept */
        facts.sort((l, r) -> this.compare(node, l, r));
    }

    @Override
    public boolean contains(PresentationGraphNode node, List<ExplicitMember> dimensions) {
        /* The dimensions of the fact must be a proper subset of the dimensions of this hypercube */
        /* We don't check members since some domains may be left unspecified in the presentation network */
        Set<Concept> factDimensions = dimensions.stream().map(ExplicitMember::getDimension).collect(Collectors.toSet());

        assert !factDimensions.isEmpty();
        PresentationHypercube hypercube = selectHypercube(node);
        Set<Concept> axes = hypercube.getAxes().stream().map(PresentationHypercube.PresentationAxis::getDimension)
                .map(PresentationGraphNode::getConcept).collect(Collectors.toSet());
        factDimensions.removeAll(axes);
        return factDimensions.isEmpty();
    }

    private PresentationHypercube.PresentationAxis getAxis(PresentationGraphNode node, Concept axis) {
        PresentationHypercube hypercube = selectHypercube(node);
        List<PresentationHypercube.PresentationAxis> axes = hypercube.getAxes();
        for (PresentationHypercube.PresentationAxis presentationAxis : axes) {
            PresentationGraphNode axisNode = presentationAxis.getDimension();
            if (axisNode.getConcept().equals(axis)) {
                return presentationAxis;
            }
        }

        return null;
    }

    @Override
    public String getAxisLabel(PresentationGraphNode node, Concept axis) {
        Label label;

        PresentationHypercube.PresentationAxis presentationAxis = getPresentationAxis(node, axis);
        if (presentationAxis != null) {
            PresentationGraphNode axisNode = presentationAxis.getDimension();
            PresentationArc arc = axisNode.getArc();
            if (arc != null && arc.getPreferredLabel() != null) {
                label = axisNode.getConcept().getLabel(arc.getPreferredLabel());
                if (label != null) {
                    return label.getValue();
                } else {
                    logMissingLabel(arc, axisNode.getConcept());
                }
            }
        }

        label = axis.getLabel(Label.ROLE_TYPE_TERSE_LABEL);
        if (label != null)
            return label.getValue();

        label = axis.getLabel();
        if (label != null)
            return label.getValue();

        log.info("Label missing for Axis [{}]", axis.getQualifiedName());
        return axis.getQualifiedName();
    }

    @Override
    public LabelPair getLabel(PresentationGraphNode node, ExplicitMember pair) {
        PresentationHypercube.PresentationAxis presentationAxis = getPresentationAxis(node, pair.getDimension());
        String axisLabel = getAxisLabel(node, pair.getDimension());

        Label label = null;
        if (presentationAxis != null) {
            for (PresentationGraphNode member : presentationAxis.getMembers()) {
                if (!member.getConcept().equals(pair.getMember()))
                    continue;

                PresentationArc arc = member.getArc();
                if (arc != null && arc.getPreferredLabel() != null) {
                    label = member.getConcept().getLabel(arc.getPreferredLabel());
                }
            }
        }
        if (label != null)
            return new LabelPair(axisLabel, label.getValue());

        Concept concept = pair.getMember();
        label = concept.getLabel(Label.ROLE_TYPE_TERSE_LABEL);
        if (label != null)
            return new LabelPair(axisLabel, label.getValue());

        label = concept.getLabel();
        if (label != null)
            return new LabelPair(axisLabel, label.getValue());

        log.info("Label missing for Member [{}]", concept.getQualifiedName());
        return new LabelPair(axisLabel, concept.getQualifiedName());
    }

    @Override
    public List<PresentationGraphNode> getLineItems() {
        if (hypercubes.size() == 1) {
            return hypercubes.get(0).getLineItems();
        }

        List<PresentationGraphNode> lineItems = new ArrayList<>();
        for (PresentationHypercube hypercube : hypercubes) {
            lineItems.addAll(hypercube.getLineItems());
        }
        return lineItems;
    }

    private static int level(PresentationGraphNode graphNode, Concept axis) {
        int level = 0;
        PresentationGraphNode node = graphNode;

        while (!node.getConcept().equals(axis) && Objects.nonNull(node.getParent())) {
            if (!node.isDomain())
                ++level;
            node = (PresentationGraphNode) node.getParent();
        }

        return level;
    }

    private static final String SRT_PRODUCT_OR_SERVICE_AXIS = "srt:ProductOrServiceAxis";

    private static boolean isStandardAxis(ExplicitMember em) {
        return em.getDimension().getQualifiedName().equals(SRT_PRODUCT_OR_SERVICE_AXIS);
    }

    @Override
    public int level(PresentationGraphNode node, List<ExplicitMember> dimensions) {
        ExplicitMember last = dimensions.get(dimensions.size() - 1);

        PresentationHypercube.PresentationAxis axis = getPresentationAxis(node, last.getDimension());
        assert Objects.nonNull(axis);
        for (PresentationGraphNode member : axis.getMembers()) {
            if (member.getConcept().equals(last.getMember())) {
                return level(member, last.getDimension()) + dimensions.size() - 1;  /* TODO The return value is sometimes off by 1 */
            }
        }

        /* standard axis such as srt:ProductOrServiceAxis are sometimes incomplete - in such cases, return the level of the first member in the axis, e.g. BRK.B FY 2022 */
        if (isStandardAxis(last) && !axis.getMembers().isEmpty()) {
            return level(axis.getMembers().get(0), last.getDimension()) + dimensions.size() - 1;
        }

        return 0;
    }
}
