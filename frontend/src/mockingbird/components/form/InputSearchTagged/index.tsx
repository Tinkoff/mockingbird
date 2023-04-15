import React, { useCallback } from 'react';
import type { Control } from 'react-hook-form';
import { useController } from 'react-hook-form';
import type { MultiSelectProps } from '@mantine/core';
import { MultiSelect } from '@mantine/core';
import {
  mapSelectValue,
  extractError,
} from 'src/mockingbird/infrastructure/helpers/forms';

type Props = Omit<MultiSelectProps, 'data' | 'name'> & {
  name: string;
  options: string[];
  control: Control;
};

export default function InputSearchTagged(props: Props) {
  const {
    name,
    label,
    placeholder,
    options = [],
    control,
    required = false,
    disabled = false,
    ...restProps
  } = props;
  const { field, formState, fieldState } = useController({
    name,
    control,
    rules: {
      required,
    },
    defaultValue: options,
  });
  const { value, onChange } = field;
  const handleChange = useCallback(
    (val) => onChange(val.map(mapSelectValue)),
    [onChange]
  );
  const uiError =
    extractError(name, formState.errors) || fieldState.error?.message;
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { ref, ...fieldProps } = field;
  return (
    <MultiSelect
      {...restProps}
      {...fieldProps}
      label={label}
      placeholder={placeholder}
      disabled={disabled}
      data={value}
      onChange={handleChange}
      error={uiError}
      searchable
      creatable
      getCreateLabel={(query) => `+ Добавить ${query}`}
      onCreate={(q) => {
        const query = q.trim().toLowerCase();
        const item = { value: query, label: q };
        onChange([...value, item]);
        return item;
      }}
    />
  );
}
