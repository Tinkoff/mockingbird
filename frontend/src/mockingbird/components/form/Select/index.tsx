import React, { useCallback } from 'react';
import type { Control } from 'react-hook-form';
import { useController } from 'react-hook-form';
import type { SelectProps } from '@platform-ui/select';
import PlatformSelect from '@platform-ui/select';

export type Props = SelectProps & {
  control: Control;
};

export default function Select(props: Props) {
  const { name, control, required = false } = props;
  const { field } = useController({
    name,
    control,
    rules: {
      required,
    },
    defaultValue: '',
  });
  const { onChange } = field;
  const handleChange = useCallback(
    (_, { value }) => onChange(value),
    [onChange]
  );
  return (
    <PlatformSelect
      {...props}
      {...field}
      onChange={handleChange}
      required={required}
    />
  );
}
