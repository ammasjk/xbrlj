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

import io.datanapis.xbrl.model.Concept;
import io.datanapis.xbrl.model.DimensionedFact;
import io.datanapis.xbrl.model.Instant;
import io.datanapis.xbrl.model.Period;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

public class PeriodGroup implements Comparable<PeriodGroup> {
    private final Instant start;
    private final Period current;
    private final Instant end;
    private final Collection<DimensionedFact> startingFacts;
    private final Collection<DimensionedFact> currentFacts;
    private final Collection<DimensionedFact> endingFacts;

    private PeriodGroup(Builder builder) {
        this.start = builder.start;
        this.startingFacts = builder.startingFacts;

        this.current = builder.current;
        this.currentFacts = builder.currentFacts;

        this.end = builder.end;
        this.endingFacts = builder.endingFacts;
    }

    public Instant getStart() {
        return start;
    }
    public Collection<DimensionedFact> getStartingFacts() {
        return startingFacts;
    }

    public Period getCurrent() {
        return current;
    }
    public Collection<DimensionedFact> getCurrentFacts() {
        return currentFacts;
    }

    public Instant getEnd() {
        return end;
    }
    public Collection<DimensionedFact> getEndingFacts() {
        return endingFacts;
    }

    private static int size(Collection<DimensionedFact> collection) {
        return (collection == null) ? 0 : collection.size();
    }

    public int getTotalFacts() {
        return size(startingFacts) + size(currentFacts) + size(endingFacts);
    }

    @Override
    public int compareTo(@NotNull PeriodGroup rhs) {
        return this.current.compareTo(rhs.current);
    }

    public static class Builder {
        private Instant start = null;
        private Collection<DimensionedFact> startingFacts = null;

        private Period current = null;
        private Collection<DimensionedFact> currentFacts = null;

        private Instant end = null;
        private Collection<DimensionedFact> endingFacts = null;

        public Builder() {
        }
        public Builder start(Instant start) {
            this.start = start;
            return this;
        }
        public Builder current(Period current) {
            this.current = current;
            return this;
        }
        public Builder end(Instant end) {
            this.end = end;
            return this;
        }
        public Builder startingFacts(Collection<DimensionedFact> startingFacts, Set<Concept> filter) {
            this.startingFacts = startingFacts.stream().filter(df -> filter.contains(df.getFact().getConcept())).toList();
            return this;
        }
        public Builder currentFacts(Collection<DimensionedFact> currentFacts) {
            this.currentFacts = currentFacts;
            return this;
        }
        public Builder endingFacts(Collection<DimensionedFact> endingFacts) {
            this.endingFacts = endingFacts;
            return this;
        }
        public PeriodGroup build() {
            return new PeriodGroup(this);
        }
    }
}
