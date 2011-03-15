/*
 * Copyright (c) 2011 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.mapping.model;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class Association {

  protected PersistentProperty inverse;
  protected PersistentProperty obverse;

  public Association(PersistentProperty inverse, PersistentProperty obverse) {
    this.inverse = inverse;
    this.obverse = obverse;
  }

  public PersistentProperty getInverse() {
    return inverse;
  }

  public void setInverse(PersistentProperty inverse) {
    this.inverse = inverse;
  }

  public PersistentProperty getObverse() {
    return obverse;
  }

  public void setObverse(PersistentProperty obverse) {
    this.obverse = obverse;
  }
}
