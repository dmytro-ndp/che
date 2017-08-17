/*******************************************************************************
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package renameMethodsInInterface.test20;
interface I {
    void m();
}
interface J{
    void m();
}
interface J2 extends J{
    void m();
}

class A{
    public void m(){}
}
class C extends A implements I, J{
}
