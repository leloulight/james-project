/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.jmap.model;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

@JsonDeserialize(builder = FilterOperator.Builder.class)
public class FilterOperator implements Filter {

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private Operator operator;
        private final ImmutableList.Builder<Filter> conditionsBuilder;

        private Builder() {
            conditionsBuilder = ImmutableList.builder();
        }

        public Builder operator(Operator operator) {
            Preconditions.checkNotNull(operator);
            this.operator = operator;
            return this;
        }

        public Builder conditions(List<Filter> conditions) {
            this.conditionsBuilder.addAll(conditions);
            return this;
        }

        public FilterOperator build() {
            Preconditions.checkState(operator != null, "'operator' is mandatory");
            ImmutableList<Filter> conditions = conditionsBuilder.build();
            Preconditions.checkState(!conditions.isEmpty(), "'conditions' is mandatory");
            return new FilterOperator(operator, conditions);
        }
    }

    private final Operator operator;
    private final List<Filter> conditions;

    @VisibleForTesting FilterOperator(Operator operator, List<Filter> conditions) {
        this.operator = operator;
        this.conditions = conditions;
    }

    public Operator getOperator() {
        return operator;
    }

    public List<Filter> getConditions() {
        return conditions;
    }
}
