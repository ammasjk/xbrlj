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

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Axis represents a set of members along one dimension
 */
@Getter
public class Axis  {
    private final Concept dimension;
    private final List<Concept> domains;
    @Setter
    private Concept defaultDomain;
    private final LinkedHashSet<Concept> members;

    public Axis(Concept dimension) {
        this.dimension = dimension;
        this.domains = new ArrayList<>();
        this.members = new LinkedHashSet<>();
    }

    public boolean hasDefaultDomain() {
        return defaultDomain != null;
    }

    public void addDomain(Concept domain) {
        this.domains.add(domain);
    }

    public void addMember(Concept member) {
        members.add(member);
    }

    public boolean hasMember(Concept member) {
        return members.contains(member);
    }

    public boolean hasMemberAsDomain(Concept member) {
        /* Some instances incorrectly classify a member as a domain */
        long count = domains.stream().filter(concept -> concept.equals(member)).count();
        return count > 0;
    }

    public Set<Concept> getMembers() {
        return members;
    }

    public Set<ExplicitMember> getExplicitMembers() {
        Set<ExplicitMember> explicitMembers = new LinkedHashSet<>();
        for (Concept member : members) {
            explicitMembers.add(new ExplicitMember(dimension, member));
        }
        return explicitMembers;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(dimension.getQualifiedName());
        if (defaultDomain != null) {
            builder.append("[").append(defaultDomain.getQualifiedName()).append("]");
        }
        builder.append("(");
        boolean first = true;
        for (Concept member : members) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(member.getQualifiedName());
            first = false;
        }
        builder.append(")");
        return builder.toString();
    }
}
