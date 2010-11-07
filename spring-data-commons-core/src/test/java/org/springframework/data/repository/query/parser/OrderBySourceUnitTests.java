/*
 * Copyright 2008-2010 the original author or authors.
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
package org.springframework.data.repository.query.parser;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.domain.Sort.Direction.*;

import org.junit.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;


/**
 * Unit test for {@link OrderBySource}.
 * 
 * @author Oliver Gierke
 */
public class OrderBySourceUnitTests {

    @Test
    public void handlesSingleDirectionAndPropertyCorrectly() throws Exception {

        assertThat(new OrderBySource("UsernameDesc").toSort(), is(new Sort(
                DESC, "username")));
    }


    @Test
    public void handlesSingleDirectionAndMultiplePropertiesCorrectly()
            throws Exception {

        assertThat(new OrderBySource("LastnameUsernameDesc").toSort(),
                is(new Sort(DESC, "lastname", "username")));
    }


    @Test
    public void handlesMultipleDirectionsCorrectly() throws Exception {

        OrderBySource orderBySource =
                new OrderBySource("LastnameAscUsernameDesc");
        assertThat(orderBySource.toSort(), is(new Sort(new Order(ASC,
                "lastname"), new Order(DESC, "username"))));
    }


    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingProperty() throws Exception {

        new OrderBySource("Desc");
    }
}
