/**
 * Copyright 2015 ArcBees Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.arcbees.chosen.client.gwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.arcbees.chosen.client.ChosenOptions;
import com.arcbees.chosen.client.event.ChosenChangeEvent;
import com.arcbees.chosen.client.event.ChosenChangeEvent.ChosenChangeHandler;
import com.google.common.base.Preconditions;
import com.google.common.collect.Ordering;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.view.client.ProvidesKey;

public abstract class BaseChosenValueListBox<T> extends Composite implements Focusable, HasEnabled,
        ChosenChangeHandler {
    protected final ProvidesKey<T> keyProvider;
    protected final List<T> values = new ArrayList<T>();
    protected final Map<Object, Integer> valueKeyToIndex = new HashMap<Object, Integer>();

    public BaseChosenValueListBox(ProvidesKey<T> keyProvider, ChosenOptions options) {
        Preconditions.checkNotNull(keyProvider);
        Preconditions.checkNotNull(options);

        this.keyProvider = keyProvider;

        initWidget(createChosenListBox(options));

        getChosenListBox().addChosenChangeHandler(this);
    }

    /**
     * Add a value to the acceptable values list. This method will update the component automatically. Please use
     * {@link #addValues(java.util.List)} to add a list of values.
     */
    public void addValue(T value) {
        doAddValue(value);

        getChosenListBox().update();
    }

    /**
     * Add values to the acceptable values list. This method will update the component automatically.
     */
    public void addValues(List<T> valuesToAdd) {
        for (T value : valuesToAdd) {
            doAddValue(value);
        }

        getChosenListBox().update();
    }

    @Override
    public int getTabIndex() {
        return getChosenListBox().getTabIndex();
    }

    @Override
    public void setTabIndex(int index) {
        getChosenListBox().setTabIndex(index);
    }

    /**
     * Return true if the value is part of the accepted values list of this component.
     */
    public boolean isAccepted(T value) {
        return valueKeyToIndex.containsKey(value);
    }

    @Override
    public boolean isEnabled() {
        return getChosenListBox().isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        getChosenListBox().setEnabled(enabled);
    }

    @Override
    public void onChange(ChosenChangeEvent event) {
        if (event.isSelection()) {
            selectValue(values.get(event.getIndex()));
        } else {
            deselectValue(values.get(event.getIndex()));
        }
    }

    /**
     * Remove a value to the acceptable values list. This method will update the component automatically. Please use
     * {@link #removeValues(java.util.List)} to remove a list of values.
     * <p/>
     * Calling this method will not reset the current selected value(s) except if this(ese) value(s) is(are) not in
     * the accepted values list anymore.
     */
    public boolean removeValue(T value) {
        Object key = keyProvider.getKey(value);

        if (!valueKeyToIndex.containsKey(key)) {
            return false;
        }

        int index = valueKeyToIndex.remove(key);
        values.remove(index);
        getChosenListBox().removeItem(index);

        updateAfterRemoval();

        return true;
    }

    /**
     * Remove values to the acceptable values list. This method will update the component automatically.
     * <p/>
     * Calling this method will not reset the current selected value(s) except if this(ese) value(s) is(are) not in
     * the accepted values list anymore.
     */
    public void removeValues(List<T> valuesToRemove) {
        // we have to remove values in decreasing order of their related index. Otherwise, we will have to update
        // the indexes map each time we remove an item and this method will perform in O(n2)
        TreeSet<Integer> indexToRemove = new TreeSet<Integer>(Ordering.natural().reverse());

        for (T value : valuesToRemove) {
            Object key = keyProvider.getKey(value);
            if (valueKeyToIndex.containsKey(key)) {
                indexToRemove.add(valueKeyToIndex.get(key));
            }
        }

        if (!indexToRemove.isEmpty()) {
            for (int index : indexToRemove) {
                removeItem(index);
            }

            updateAfterRemoval();
        }
    }

    /**
     * Set the list of the values that will be accepted by the widget.
     * <p/>
     * Calling this method will not reset the current selected value(s) except if this(ese) value(s) is(are) not in
     * the accepted values list anymore.
     * <p/>
     * With {@link com.arcbees.chosen.client.gwt.ChosenValueListBox}, if you want to display the placeHolder message
     * or be able to reset the component by passing null. You have to pass null as first element of your collections
     */
    public void setAcceptableValues(Collection<T> acceptableValues) {
        values.clear();
        valueKeyToIndex.clear();
        ChosenListBox listBox = getChosenListBox();
        listBox.clear(false);

        for (T nextNewValue : acceptableValues) {
            doAddValue(nextNewValue);
        }

        updateChosenListBox();
    }

    @Override
    public void setAccessKey(char key) {
        getChosenListBox().setAccessKey(key);
    }

    @Override
    public void setFocus(boolean focused) {
        getChosenListBox().setFocus(focused);
    }

    /**
     * Add the item to the ChosenListBox. Override this method if you want to implement your custom way to add
     * the item in the ChosenListBox (setting a style class for the item for example)
     */
    protected abstract void addItemToChosenListBox(T value);

    /**
     * Create the ChosenListBox component that will be used in this widget.
     */
    protected abstract ChosenListBox createChosenListBox(ChosenOptions options);

    /**
     * Method called when the user deselects a value with the ChosenListBox in multiple select mode.
     */
    protected abstract void deselectValue(T value);

    /**
     * Return the ChosenListBox used by this widget.
     * <p/>
     * This method can return {@code null} if the widget is not fully initialized
     * (before the return of the constructor)
     *
     * @return the ChosenListBox used by this widget
     */
    protected ChosenListBox getChosenListBox() {
        return (ChosenListBox) getWidget();
    }

    /**
     * Method called when the user selects a value with the ChosenListBox.
     */
    protected abstract void selectValue(T value);

    /**
     * Update the ChosenListBox with the value(s) set by the developer.
     */
    protected abstract void updateChosenListBox();

    private void doAddValue(T value) {
        Object key = keyProvider.getKey(value);
        Preconditions.checkState(!valueKeyToIndex.containsKey(key), "Duplicate value: %s", value);

        valueKeyToIndex.put(key, values.size());
        values.add(value);

        addItemToChosenListBox(value);
    }

    private void removeItem(int index) {
        values.remove(index);
        getChosenListBox().removeItem(index);
    }

    private void updateAfterRemoval() {
        updateIndex();
        updateChosenListBox();
    }

    private void updateIndex() {
        valueKeyToIndex.clear();

        for (int i = 0; i < values.size(); i++) {
            valueKeyToIndex.put(keyProvider.getKey(values.get(i)), i);
        }
    }
}
