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
package io.datanapis.xbrl.model;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DimensionedFact - a fact with dimensions. Dimensions qualify a fact. For example, us-gaap:Revenue can be reported
 * against the business as a whole, but it can also be reported for the different operating segments, for the different
 * products, etc. Dimensions help qualify this. The Concept is still us-gaap:Revenue. However, the dimensions will
 * qualify the context this concept is being used in.
 */
public final class DimensionedFact implements Comparable<DimensionedFact> {
    private final Fact fact;
    private final List<ExplicitMember> dimensions;

    public DimensionedFact(Fact fact) {
        this.fact = fact;
        this.dimensions = null;
    }

    public DimensionedFact(Fact fact, List<ExplicitMember> dimensions) {
        if (dimensions == null)
            throw new NullPointerException("dimensions cannot be null!");

        this.fact = fact;
        this.dimensions = dimensions;
    }

    public Fact getFact() {
        return fact;
    }

    public List<ExplicitMember> getDimensions() {
        return dimensions;
    }

    public Collection<TypedMember> getTypedMembers() {
        return fact.getContext().getTypedMembers();
    }

    public boolean isQualified() {
        return (getDimensions() != null && !getDimensions().isEmpty()) || !getTypedMembers().isEmpty();
    }

    public static List<DimensionedFact> getDistinctFacts(Collection<DimensionedFact> facts) {
        return facts.stream().sorted().distinct().collect(Collectors.toList());
    }

    @Override
    public String toString() {
        if (!isQualified())
            return fact.toString();

        String suffix;
        if (dimensions != null) {
            suffix = "/[D" + dimensions.size() + "]";
        } else {
            suffix = "/[T" + getTypedMembers().size() + "]";
        }
        return fact.toString() + suffix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DimensionedFact other = (DimensionedFact) o;

        if (!fact.equals(other.fact)) return false;
        if (dimensions == null) {
            return other.dimensions == null;
        } else {
            return dimensions.equals(other.dimensions);
        }
    }

    @Override
    public int hashCode() {
        int result = fact.hashCode();
        result = 31 * result + (dimensions != null ? dimensions.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(@NotNull DimensionedFact o) {
        if (dimensions == null) {
            if (o.dimensions == null || o.dimensions.isEmpty()) {
                return Fact.compare(fact, o.getFact());
            } else {
                /* left has fewer qualifiers */
                return -1;
            }
        } else if (o.dimensions == null || o.dimensions.isEmpty()) {
            if (dimensions.isEmpty()) {
                return Fact.compare(fact, o.getFact());
            } else {
                return 1;
            }
        }

        Iterator<ExplicitMember> i = dimensions.iterator();
        Iterator<ExplicitMember> j = o.dimensions.iterator();
        while (i.hasNext() && j.hasNext()) {
            ExplicitMember lhs = i.next();
            ExplicitMember rhs = j.next();
            int result = lhs.compareTo(rhs);
            if (result != 0) {
                return result;
            }
        }

        if (i.hasNext()) {
            /* left has more qualifiers */
            return 1;
        } else if (j.hasNext()) {
            return -1;
        }

        return Fact.compare(fact, o.getFact());
    }
}
