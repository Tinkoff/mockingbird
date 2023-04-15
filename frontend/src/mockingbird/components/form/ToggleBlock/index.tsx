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
    required = false,
    labelPosition,
    ...restProps
  } = props;
  const { field } = useController({
    name,
    control,
    rules: {
      required,
    },
    defaultValue: false,
  });
  const { onChange } = field;
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
      label={label}
      labelPosition={labelPosition}
      onChange={handleChange}
    />
  );
}
