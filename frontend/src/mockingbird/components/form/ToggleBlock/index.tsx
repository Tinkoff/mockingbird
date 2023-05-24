import React, { useCallback } from 'react';
import type { Control } from 'react-hook-form';
import { useController } from 'react-hook-form';
import type { SwitchProps } from '@mantine/core';
import { Switch } from '@mantine/core';

type Props = Omit<SwitchProps, 'name'> & {
  name: string;
  control: Control;
};

export default function ToggleBlock(props: Props) {
  const {
    name,
    label,
    control,
    labelPosition,
    required = false,
    defaultValue = false,
    ...restProps
  } = props;
  const { field } = useController({
    name,
    control,
    rules: {
      required,
    },
    defaultValue,
  });
  const { onChange, value } = field;
  const handleChange = useCallback(
    (event) => {
      onChange(event.currentTarget.checked);
    },
    [onChange]
  );
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { ref, ...fieldProps } = field;
  return (
    <Switch
      {...restProps}
      {...fieldProps}
      checked={value}
      label={label}
      labelPosition={labelPosition}
      onChange={handleChange}
    />
  );
}
