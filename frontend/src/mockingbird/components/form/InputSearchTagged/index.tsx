import React, { useCallback } from 'react';
import type { Control } from 'react-hook-form';
import { useController } from 'react-hook-form';
import PlatformInputSearchTagged from '@platform-ui/inputSearchTagged';
import {
  mapSelectItem,
  mapSelectValue,
} from 'src/mockingbird/infrastructure/helpers/forms';

interface Props {
  name: string;
  label: string;
  options: any[];
  control: Control;
  required?: boolean;
  disabled?: boolean;
}

export default function InputSearchTagged(props: Props) {
  const {
    name,
    label,
    options,
    control,
    required = false,
    disabled = false,
  } = props;
  const { field } = useController({
    name,
    control,
    rules: {
      required,
    },
    defaultValue: [],
  });
  const { value, onChange } = field;
  const handleRequest = useCallback(
    (query) => {
      const q = query.trim().toLowerCase();
      const currentOptions = value.length
        ? options.filter((i) => !value.includes(i))
        : options;
      return q
        ? currentOptions
            .filter((i) => i.toLocaleString().includes(q))
            .map(mapSelectItem)
        : currentOptions.map(mapSelectItem);
    },
    [options, value]
  );
  const handleChange = useCallback(
    (_, { value: newValue }) => onChange(newValue.map(mapSelectValue)),
    [onChange]
  );
  const { ref, ...fieldProps } = field;
  return (
    <PlatformInputSearchTagged
      {...fieldProps}
      onChange={handleChange}
      request={handleRequest}
      label={label}
      disabled={disabled}
    />
  );
}
