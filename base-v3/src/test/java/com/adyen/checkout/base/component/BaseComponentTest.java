/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 10/4/2019.
 */

package com.adyen.checkout.base.component;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.adyen.checkout.base.DataProvider;
import com.adyen.checkout.base.PaymentComponentState;
import com.adyen.checkout.base.model.paymentmethods.PaymentMethod;
import com.adyen.checkout.base.model.payments.request.PaymentComponentData;
import com.adyen.checkout.base.model.payments.request.PaymentMethodDetails;
import com.adyen.checkout.base.models.TestConfiguration;
import com.adyen.checkout.base.models.TestInputData;
import com.adyen.checkout.base.models.TestOutputData;
import com.adyen.checkout.base.models.TestPaymentMethod;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.io.IOException;

public class BaseComponentTest {

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    BasePaymentComponent<TestConfiguration, TestInputData, TestOutputData, PaymentComponentState<? extends PaymentMethodDetails>> mBaseComponent;

    PaymentMethod paymentMethod;
    PaymentMethodDelegateTest mPaymentMethodDelegateTest = new PaymentMethodDelegateTest();
    ClassLoader classLoader;

    @Before
    public void init() throws IOException, JSONException {
        classLoader = this.getClass().getClassLoader();
        paymentMethod = DataProvider.getPaymentMethodResponse(classLoader).getPaymentMethods().get(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void initBaseComponent_notSupportedPaymentMethod_expectException() throws IOException, JSONException {
        mBaseComponent = new BasePaymentComponent<
                TestConfiguration, TestInputData, TestOutputData, PaymentComponentState<? extends PaymentMethodDetails>>(
                    new PaymentMethodDelegateTest(),
                    null) {
            @NonNull
            @Override
            protected TestOutputData onInputDataChanged(@NonNull TestInputData inputData) {
                return new TestOutputData();
            }

            @NonNull
            @Override
            protected PaymentComponentState<? extends PaymentMethodDetails> createComponentState() {
                return null;
            }

            @NonNull
            @Override
            public String[] getSupportedPaymentMethodTypes() {
                return new String[]{"something"};
            }
        };
    }

    @Test
    public void initBaseComponent_SupportedPaymentMethod_expectOutputData() {
        mBaseComponent = getBaseComponent();

        mBaseComponent.observeOutputData(mockLifecycleOwner(), Assert::assertNotNull);
        Assert.assertNull(mBaseComponent.getOutputData());
    }

    @Test
    public void initBaseComponent_ChangeInputData_expectPaymentDetails() {
        mBaseComponent = getBaseComponent();
        mBaseComponent.inputDataChanged(new TestInputData());

        mBaseComponent.observe(mockLifecycleOwner(), paymentComponentState -> Assert.assertNotNull(paymentComponentState.getData()));
    }

    @Test
    public void initBaseComponent_SupportedPaymentMethod_latePaymentDetails() {
        mBaseComponent = getBaseComponent();
        mBaseComponent.inputDataChanged(new TestInputData(false));

        mBaseComponent.observe(mockLifecycleOwner(), paymentComponentState -> Assert.assertEquals(1, 1));
    }

    private BasePaymentComponent<TestConfiguration, TestInputData, TestOutputData, PaymentComponentState<? extends PaymentMethodDetails>> getBaseComponent() {
        BasePaymentComponent<TestConfiguration, TestInputData, TestOutputData, PaymentComponentState<? extends PaymentMethodDetails>> baseComponent =
                new BasePaymentComponent<TestConfiguration, TestInputData, TestOutputData, PaymentComponentState<? extends PaymentMethodDetails>>
                        (mPaymentMethodDelegateTest, null) {
            @NonNull
            @Override
            protected TestOutputData onInputDataChanged(@NonNull TestInputData inputData) {
                TestOutputData outputData = new TestOutputData(inputData.isValid);
                return outputData;
            }

            @NonNull
            @Override
            protected PaymentComponentState<TestPaymentMethod> createComponentState() {
                final PaymentComponentData<TestPaymentMethod> paymentComponentData = new PaymentComponentData<>();
                paymentComponentData.setPaymentMethod(new TestPaymentMethod());
                return new PaymentComponentState<>(paymentComponentData, true);
            }

            @NonNull
            @Override
            public String[] getSupportedPaymentMethodTypes() {
                return new String[]{""};
            }
        };

        return baseComponent;
    }

    private static LifecycleOwner mockLifecycleOwner() {
        LifecycleOwner owner = mock(LifecycleOwner.class);
        LifecycleRegistry lifecycle = new LifecycleRegistry(owner);
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        when(owner.getLifecycle()).thenReturn(lifecycle);
        return owner;

    }
}